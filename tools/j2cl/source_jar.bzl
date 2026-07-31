def _j2cl_source_jar_impl(ctx):
    output = ctx.actions.declare_file(ctx.label.name + ".jar")
    args = ctx.actions.args()
    args.add("--input", ctx.file.src)
    args.add("--output", output)
    if ctx.attr.activate_j2cl_only:
        args.add("--activate-j2cl-only")
    if ctx.attr.rewrite_akasha_window_global:
        args.add("--rewrite-akasha-window-global")
    if ctx.attr.rewrite_arez_global_console:
        args.add("--rewrite-arez-global-console")
    for prefix in ctx.attr.exclude_prefixes:
        args.add("--exclude-prefix", prefix)

    ctx.actions.run(
        arguments = [args],
        executable = ctx.executable._builder,
        inputs = [ctx.file.src],
        mnemonic = "SanitizeJ2clSourceJar",
        outputs = [output],
        progress_message = "Sanitizing J2CL source jar %{label}",
    )
    return [DefaultInfo(files = depset([output]))]

j2cl_source_jar = rule(
    implementation = _j2cl_source_jar_impl,
    attrs = {
        "activate_j2cl_only": attr.bool(),
        "exclude_prefixes": attr.string_list(),
        "rewrite_akasha_window_global": attr.bool(),
        "rewrite_arez_global_console": attr.bool(),
        "src": attr.label(
            allow_single_file = [".jar"],
            mandatory = True,
        ),
        "_builder": attr.label(
            cfg = "exec",
            default = "//tools/j2cl/org/realityforge/replicant/j2cl:source_jar_builder",
            executable = True,
        ),
    },
)
