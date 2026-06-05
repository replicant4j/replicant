load("@rules_java//java:defs.bzl", _java_library = "java_library", _java_test = "java_test")

STRICT_JAVACOPTS = [
    "-XepDisableAllChecks",
    "-Werror",
    "-Xlint:all,-processing,-serial,-options,-deprecation,-this-escape",
    "-Xmaxerrs",
    "10000",
    "-Xmaxwarns",
    "10000",
]

PROCESSOR_JAVACOPTS = []

def java_library(name, javacopts = [], **kwargs):
    _java_library(
        name = name,
        javacopts = javacopts + STRICT_JAVACOPTS,
        **kwargs
    )

def java_testng(name, srcs, test_class, deps = [], runtime_deps = [], jvm_flags = [], javacopts = [], **kwargs):
    filtered_kwargs = dict(kwargs)
    for arg in [
        "main_class",
        "use_testrunner",
        "args",
    ]:
        if arg in filtered_kwargs.keys():
            filtered_kwargs.pop(arg)

    _java_test(
        name = name,
        srcs = srcs,
        use_testrunner = False,
        main_class = "org.testng.TestNG",
        args = [
            "-testclass",
            test_class,
        ],
        deps = deps + [
            "//third_party/java:testng",
        ],
        javacopts = javacopts + STRICT_JAVACOPTS,
        jvm_flags = jvm_flags + [
            "-ea",
            "-Dbraincheck.environment=development",
            "-Darez.environment=development",
            "-Dreplicant.environment=development",
        ],
        runtime_deps = runtime_deps,
        **filtered_kwargs
    )
