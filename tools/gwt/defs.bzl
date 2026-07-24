load("@rules_java//java:defs.bzl", "JavaInfo", "java_common")

GwtJavaInfo = provider(
    doc = "Java classpath and source jars collected for GWT compilation.",
    fields = {
        "classpath": "Runtime jars required by GWT.",
        "source_jars": "Source jars required by GWT, including generated sources.",
    },
)

def _as_list(value):
    if type(value) == "list":
        return value
    if value == None:
        return []
    return [value]

def _generated_source_jars(java_info):
    jars = []
    for java_output in java_info.java_outputs:
        if hasattr(java_output, "generated_source_jar") and java_output.generated_source_jar:
            jars.append(java_output.generated_source_jar)
    return jars

def _gwt_java_aspect_impl(target, ctx):
    classpath = []
    source_jars = []

    if JavaInfo in target:
        java_info = target[JavaInfo]
        classpath.append(java_info.transitive_runtime_jars)
        source_jars.append(java_info.transitive_source_jars)

        generated = _generated_source_jars(java_info)
        if generated:
            source_jars.append(depset(generated))

    for attr_name in ["deps", "exports", "runtime_deps"]:
        if hasattr(ctx.rule.attr, attr_name):
            for dep in _as_list(getattr(ctx.rule.attr, attr_name)):
                if GwtJavaInfo in dep:
                    classpath.append(dep[GwtJavaInfo].classpath)
                    source_jars.append(dep[GwtJavaInfo].source_jars)

    return [GwtJavaInfo(
        classpath = depset(transitive = classpath),
        source_jars = depset(transitive = source_jars),
    )]

_gwt_java_aspect = aspect(
    implementation = _gwt_java_aspect_impl,
    attr_aspects = [
        "deps",
        "exports",
        "runtime_deps",
    ],
)

def _stable_file_list(depsets):
    result = []
    seen = {}
    for file_depset in depsets:
        for file in sorted(file_depset.to_list(), key = lambda f: f.path):
            if file.path not in seen:
                seen[file.path] = True
                result.append(file)
    return result

def _collect_java_inputs(deps):
    classpath = []
    source_jars = []
    for dep in deps:
        if GwtJavaInfo in dep:
            classpath.append(dep[GwtJavaInfo].classpath)
            source_jars.append(dep[GwtJavaInfo].source_jars)
        elif JavaInfo in dep:
            java_info = dep[JavaInfo]
            classpath.append(java_info.transitive_runtime_jars)
            source_jars.append(java_info.transitive_source_jars)
            generated = _generated_source_jars(java_info)
            if generated:
                source_jars.append(depset(generated))
        else:
            fail("GWT deps must provide JavaInfo: %s" % dep.label)
    return classpath, source_jars

def _gwt_compile_impl(ctx):
    if not ctx.attr.modules:
        fail("gwt_compile.modules must not be empty")

    tool_classpath, tool_sources = _collect_java_inputs(ctx.attr.tool_deps)
    app_classpath, app_sources = _collect_java_inputs(ctx.attr.deps)
    classpath_files = _stable_file_list(
        tool_classpath +
        app_classpath +
        tool_sources +
        app_sources,
    )

    classpath_file = ctx.actions.declare_file(ctx.label.name + ".classpath")
    ctx.actions.write(
        output = classpath_file,
        content = "\n".join([file.path for file in classpath_files]) + "\n",
    )

    java_runtime = ctx.attr._java_runtime[java_common.JavaRuntimeInfo]

    args = ctx.actions.args()
    args.add(ctx.outputs.zip)
    args.add(classpath_file)
    args.add(ctx.attr.local_workers)
    args.add(ctx.attr.compile_report_dir)
    args.add(len(ctx.attr.jvm_args))
    args.add_all(ctx.attr.jvm_args)
    args.add(len(ctx.attr.compiler_args))
    args.add_all(ctx.attr.compiler_args)
    args.add(len(ctx.attr.modules))
    args.add_all(ctx.attr.modules)

    ctx.actions.run_shell(
        inputs = classpath_files + [classpath_file],
        outputs = [ctx.outputs.zip],
        tools = depset(
            [ctx.executable._singlejar],
            transitive = [java_runtime.files],
        ),
        arguments = [args],
        command = """
set -euo pipefail
out="$1"
classpath_file="$2"
local_workers="$3"
compile_report_dir="$4"
shift 4

tmp="$(mktemp -d "${TMPDIR:-/tmp}/gwt_compile.XXXXXX")"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/war" "$tmp/unit-cache"

classpath=""
while IFS= read -r entry; do
  if [[ -n "$entry" ]]; then
    absolute="$PWD/$entry"
    if [[ -z "$classpath" ]]; then
      classpath="$absolute"
    else
      classpath="$classpath:$absolute"
    fi
  fi
done < "$classpath_file"

java_args="$tmp/java.args"
jvm_arg_count="$1"
shift
i=0
while [[ "$i" -lt "$jvm_arg_count" ]]; do
  echo "$1" >> "$java_args"
  shift
  i=$((i + 1))
done
{
  echo "-Dgwt.persistentunitcache=true"
  echo "-Dgwt.persistentunitcachedir=$tmp/unit-cache"
  echo "-cp"
  echo "$classpath"
  echo "com.google.gwt.dev.Compiler"
  echo "-strict"
  echo "-localWorkers"
  echo "$local_workers"
  echo "-war"
  echo "$tmp/war"
} >> "$java_args"
if [[ -n "$compile_report_dir" ]]; then
  mkdir -p "$tmp/extra/$compile_report_dir"
  {
    echo "-compileReport"
    echo "-extra"
    echo "$tmp/extra/$compile_report_dir"
  } >> "$java_args"
fi

compiler_arg_count="$1"
shift
i=0
while [[ "$i" -lt "$compiler_arg_count" ]]; do
  echo "$1" >> "$java_args"
  shift
  i=$((i + 1))
done

module_count="$1"
shift
i=0
while [[ "$i" -lt "$module_count" ]]; do
  echo "$1" >> "$java_args"
  shift
  i=$((i + 1))
done

"$PWD/%s" @"$java_args"

files="$tmp/files"
find "$tmp/war" -type f | LC_ALL=C sort > "$files"
if [[ ! -s "$files" ]]; then
  echo "GWT compiler produced no WAR assets" >&2
  exit 1
fi

params="$tmp/singlejar.params"
{
  echo --output
  echo "$out"
  echo --normalize
  echo --exclude_build_data
} > "$params"
while IFS= read -r file; do
  rel="${file#$tmp/war/}"
  echo --resources >> "$params"
  echo "$file:$rel" >> "$params"
done < "$files"
"$PWD/%s" @"$params"
""" % (
            java_runtime.java_executable_exec_path,
            ctx.executable._singlejar.path,
        ),
        mnemonic = "GwtCompile",
        progress_message = "GWT-compiling %s" % ", ".join(ctx.attr.modules),
    )

    return [DefaultInfo(files = depset([ctx.outputs.zip]))]

gwt_compile = rule(
    implementation = _gwt_compile_impl,
    attrs = {
        "compile_report_dir": attr.string(),
        "compiler_args": attr.string_list(),
        "deps": attr.label_list(
            aspects = [_gwt_java_aspect],
            providers = [JavaInfo],
        ),
        "jvm_args": attr.string_list(),
        "local_workers": attr.int(default = 2),
        "modules": attr.string_list(mandatory = True),
        "tool_deps": attr.label_list(
            providers = [JavaInfo],
            mandatory = True,
        ),
        "_java_runtime": attr.label(
            default = Label("@bazel_tools//tools/jdk:current_java_runtime"),
        ),
        "_singlejar": attr.label(
            default = Label("@bazel_tools//tools/jdk:singlejar"),
            cfg = "exec",
            allow_single_file = True,
            executable = True,
        ),
    },
    outputs = {"zip": "%{name}.zip"},
)
