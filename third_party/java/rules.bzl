load(
    "@rules_java//java:defs.bzl",
    _java_binary = "java_binary",
    _java_library = "java_library",
    _java_test = "java_test",
)

_JSPECIFY = "//third_party/java:jspecify"
_NULLAWAY_PLUGIN = "//third_party/java:nullaway_plugin"

_ERROR_PRONE_JAVACOPTS = [
    "-XepExcludedPaths:(.*/external/.*|.*/_javac/.*/.*_sources/.*)",
    "-Xep:AlmostJavadoc:ERROR",
    "-Xep:AlreadyChecked:ERROR",
    "-Xep:AmbiguousMethodReference:ERROR",
    "-Xep:AnnotateFormatMethod:ERROR",
    "-Xep:ArrayAsKeyOfSetOrMap:ERROR",
    "-Xep:ArrayRecordComponent:ERROR",
    "-Xep:AssertEqualsArgumentOrderChecker:ERROR",
    "-Xep:AssertThrowsMultipleStatements:ERROR",
    "-Xep:AssignmentExpression:ERROR",
    "-Xep:AttemptedNegativeZero:ERROR",
    "-Xep:BadComparable:ERROR",
    "-Xep:BadImport:ERROR",
    "-Xep:BadInstanceof:ERROR",
    "-Xep:BareDotMetacharacter:ERROR",
    "-Xep:BigDecimalEquals:ERROR",
    "-Xep:BigDecimalLiteralDouble:ERROR",
    "-Xep:BoxedPrimitiveConstructor:ERROR",
    "-Xep:CheckedExceptionNotThrown:ERROR",
    "-Xep:ClassCanBeStatic:ERROR",
    "-Xep:ClassName:ERROR",
    "-Xep:ComparisonContractViolated:ERROR",
    "-Xep:ConstantField:ERROR",
    "-Xep:DefaultLocale:ERROR",
    "-Xep:DeprecatedVariable:ERROR",
    "-Xep:DuplicateBranches:ERROR",
    "-Xep:EmptyBlockTag:ERROR",
    "-Xep:EmptyCatch:ERROR",
    "-Xep:EmptyIf:ERROR",
    "-Xep:EmptyTopLevelDeclaration:ERROR",
    "-Xep:EqualsBrokenForNull:ERROR",
    "-Xep:EqualsMissingNullable:ERROR",
    "-Xep:FieldCanBeLocal:ERROR",
    "-Xep:FieldCanBeStatic:ERROR",
    "-Xep:Finalize:ERROR",
    "-Xep:ForEachIterable:ERROR",
    "-Xep:InconsistentHashCode:ERROR",
    "-Xep:InsecureCryptoUsage:ERROR",
    "-Xep:LongLiteralLowerCaseSuffix:ERROR",
    "-Xep:MissingBraces:ERROR",
    "-Xep:MissingOverride:ERROR",
    "-Xep:MissingRuntimeRetention:ERROR",
    "-Xep:MixedArrayDimensions:ERROR",
    "-Xep:MultiVariableDeclaration:ERROR",
    "-Xep:MultipleTopLevelClasses:ERROR",
    "-Xep:NonOverridingEquals:ERROR",
    "-Xep:NotJavadoc:ERROR",
    "-Xep:NullOptional:ERROR",
    "-Xep:NullablePrimitive:ERROR",
    "-Xep:NullablePrimitiveArray:ERROR",
    "-Xep:NullableTypeParameter:ERROR",
    "-Xep:NullableWildcard:ERROR",
    "-Xep:PackageLocation:ERROR",
    "-Xep:ParameterMissingNullable:ERROR",
    "-Xep:ParameterName:ERROR",
    "-Xep:PrimitiveArrayPassedToVarargsMethod:ERROR",
    "-Xep:PublicApiNamedStreamShouldReturnStream:ERROR",
    "-Xep:RedundantOverride:ERROR",
    "-Xep:RedundantThrows:ERROR",
    "-Xep:RemoveUnusedImports:ERROR",
    "-Xep:ReturnAtTheEndOfVoidFunction:ERROR",
    "-Xep:ReturnFromVoid:ERROR",
    "-Xep:ReturnMissingNullable:ERROR",
    "-Xep:ReturnsNullCollection:ERROR",
    "-Xep:SelfAlwaysReturnsThis:ERROR",
    "-Xep:SunApi:ERROR",
    "-Xep:SystemExitOutsideMain:ERROR",
    "-Xep:TimeUnitMismatch:ERROR",
    "-Xep:ToStringReturnsNull:ERROR",
    "-Xep:UnnecessarilyVisible:ERROR",
    "-Xep:UnnecessaryAnonymousClass:ERROR",
    "-Xep:UnnecessaryBoxedAssignment:ERROR",
    "-Xep:UnnecessaryMethodReference:ERROR",
    "-Xep:UnnecessaryOptionalGet:ERROR",
    "-Xep:UnsafeLocaleUsage:ERROR",
    "-Xep:UnsynchronizedOverridesSynchronized:ERROR",
    "-Xep:UnusedLabel:ERROR",
    "-Xep:UnusedTypeParameter:ERROR",
    "-Xep:UseCorrectAssertInTests:ERROR",
    "-Xep:UsingJsr305CheckReturnValue:ERROR",
    "-Xep:VoidMissingNullable:ERROR",
    "-Xep:BanClassLoader:ERROR",
    "-Xep:BanSerializableRead:ERROR",
    "-Xep:FieldCanBeFinal:ERROR",
    "-Xep:FieldMissingNullable:ERROR",
    "-Xep:InterruptedExceptionSwallowed:ERROR",
    "-Xep:PrivateConstructorForUtilityClass:ERROR",
    "-Xep:UnnecessaryDefaultInEnumSwitch:ERROR",
]

_TEST_ERROR_PRONE_JAVACOPTS = [
    "-Xep:MockitoDoSetup:ERROR",
]

_JAVA_JAVACOPTS = [
    "--release",
    "17",
    "-Werror",
    "-Xep:NullAway:ERROR",
    "-Xep:RequireExplicitNullMarking:ERROR",
    "-XepOpt:NullAway:OnlyNullMarked=true",
    "-XepOpt:NullAway:TreatGeneratedAsUnannotated=true",
    "-Aarez.warnings_as_errors=true",
    "-Aarez.persist.warnings_as_errors=true",
    "-Asting.warnings_as_errors=true",
    "-Areact4j.warnings_as_errors=true",
    "-Xlint:all,-processing,-serial,-path,-options,-classfile,-this-escape",
] + _ERROR_PRONE_JAVACOPTS

_SERVER_JAVACOPTS = [
    "-Xep:Varifier:ERROR",
]

_JAVA_TEST_JVM_FLAGS = [
    "-ea",
    "-Dbraincheck.environment=development",
    "-Darez.environment=development",
    "-Dreplicant.environment=development",
    "-Dzemeckis.environment=development",
]

def _with_jspecify(deps):
    return [_JSPECIFY] + deps if _JSPECIFY not in deps else deps

def _with_nullaway(plugins):
    return [_NULLAWAY_PLUGIN] + plugins if _NULLAWAY_PLUGIN not in plugins else plugins

def _has_sources(srcs):
    return len(srcs) > 0

def java_library(name, srcs = [], deps = [], plugins = [], javacopts = [], **kwargs):
    nullaway_enabled = _has_sources(srcs)
    _java_library(
        name = name,
        srcs = srcs,
        deps = _with_jspecify(deps) if nullaway_enabled else deps,
        javacopts = _JAVA_JAVACOPTS + javacopts,
        plugins = _with_nullaway(plugins) if nullaway_enabled else plugins,
        **kwargs
    )

def java_server_library(name, srcs = [], javacopts = [], **kwargs):
    java_library(
        name = name,
        srcs = srcs,
        javacopts = _SERVER_JAVACOPTS + javacopts,
        **kwargs
    )

def java_binary(name, srcs = [], deps = [], plugins = [], javacopts = [], **kwargs):
    nullaway_enabled = _has_sources(srcs)
    _java_binary(
        name = name,
        srcs = srcs,
        deps = _with_jspecify(deps) if nullaway_enabled else deps,
        javacopts = _JAVA_JAVACOPTS + _SERVER_JAVACOPTS + javacopts,
        plugins = _with_nullaway(plugins) if nullaway_enabled else plugins,
        **kwargs
    )

def java_testng(name, srcs, test_class, deps = [], runtime_deps = [], jvm_flags = [], javacopts = [], **kwargs):
    filtered_kwargs = dict(kwargs)
    for arg in [
        "main_class",
        "use_testrunner",
        "args",
    ]:
        if arg in filtered_kwargs:
            filtered_kwargs.pop(arg)

    size = filtered_kwargs.pop("size") if "size" in filtered_kwargs else "small"
    server_javacopts = _SERVER_JAVACOPTS if native.package_name().startswith("server/") else []

    _java_test(
        name = name,
        srcs = srcs + ["package-info.java"],
        use_testrunner = False,
        main_class = "org.testng.TestNG",
        args = [
            "-testclass",
            test_class,
        ],
        deps = _with_jspecify(deps + [
            "//third_party/java:testng",
        ]),
        javacopts = _JAVA_JAVACOPTS + _TEST_ERROR_PRONE_JAVACOPTS + server_javacopts + javacopts,
        jvm_flags = _JAVA_TEST_JVM_FLAGS + jvm_flags,
        plugins = [_NULLAWAY_PLUGIN],
        runtime_deps = runtime_deps,
        size = size,
        **filtered_kwargs
    )
