load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_java//java/common:java_info.bzl", "JavaInfo")

def _is_third_party_jar(ctx, file):
    owner = file.owner
    if owner.workspace_name and owner.workspace_name != ctx.label.workspace_name:
        return True
    package = owner.package
    return package == "third_party" or package.startswith("third_party/")

def _split_jars(ctx, files):
    non_third_party_jars = []
    third_party_jars = []
    for file in files:
        if not file.extension == "jar":
            fail("unexpected file type in java_single_jar.deps: %s" % file.path)
        if _is_third_party_jar(ctx, file):
            third_party_jars.append(file)
        else:
            non_third_party_jars.append(file)
    return depset(non_third_party_jars), depset(third_party_jars)

def _java_single_jar(ctx):
    transitive_inputs = []
    transitive_third_party_inputs = []
    for dep in ctx.attr.deps:
        if JavaInfo in dep:
            files = dep[JavaInfo].transitive_runtime_jars.to_list()
        else:
            files = dep[DefaultInfo].files.to_list()
        runtime_jars, third_party_jars = _split_jars(ctx, files)
        transitive_inputs.append(runtime_jars)
        transitive_third_party_inputs.append(third_party_jars)

    inputs = depset(transitive = transitive_inputs)
    third_party_inputs = depset(transitive = transitive_third_party_inputs)

    args = ctx.actions.args()
    args.add_all("--sources", inputs)
    args.use_param_file("@%s")
    args.set_param_file_format("multiline")
    args.add_all("--deploy_manifest_lines", ctx.attr.deploy_manifest_lines)
    args.add("--output", ctx.outputs.jar)
    args.add("--normalize")

    if ctx.attr.compress == "preserve":
        args.add("--dont_change_compression")
    elif ctx.attr.compress == "yes":
        args.add("--compression")
    elif ctx.attr.compress == "no":
        pass
    else:
        fail("\"compress\" attribute (%s) must be: yes, no, preserve." % ctx.attr.compress)

    if ctx.attr.exclude_build_data:
        args.add("--exclude_build_data")
    if ctx.attr.multi_release:
        args.add("--multi_release")

    ctx.actions.run(
        inputs = inputs,
        outputs = [ctx.outputs.jar],
        arguments = [args],
        progress_message = "Merging into %s" % ctx.outputs.jar.short_path,
        mnemonic = "JavaSingleJar",
        executable = ctx.executable._singlejar,
    )

    third_party_jar_files = sorted(third_party_inputs.to_list(), key = lambda file: file.path)
    third_party_jar_list = "\n".join([file.short_path for file in third_party_jar_files])
    if third_party_jar_list:
        third_party_jar_list += "\n"
    ctx.actions.write(output = ctx.outputs.third_party_jars, content = third_party_jar_list)

    files = depset(transitive = [depset([ctx.outputs.jar, ctx.outputs.third_party_jars]), third_party_inputs])

    providers = [DefaultInfo(
        files = files,
        runfiles = ctx.runfiles(transitive_files = files),
    )]
    if hasattr(java_common, "JavaRuntimeClasspathInfo"):
        providers.append(java_common.JavaRuntimeClasspathInfo(runtime_classpath = inputs))
    return providers

java_single_jar = rule(
    attrs = {
        "compress": attr.string(default = "preserve"),
        "deploy_manifest_lines": attr.string_list(),
        "deps": attr.label_list(
            providers = [JavaInfo],
            allow_files = True,
        ),
        "exclude_build_data": attr.bool(default = True),
        "multi_release": attr.bool(default = True),
        "_singlejar": attr.label(
            default = Label("@bazel_tools//tools/jdk:singlejar"),
            cfg = "exec",
            allow_single_file = True,
            executable = True,
        ),
    },
    outputs = {
        "jar": "%{name}.jar",
        "third_party_jars": "%{name}.third_party_jars.txt",
    },
    implementation = _java_single_jar,
)
