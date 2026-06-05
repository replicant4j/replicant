load("@rules_java//java:defs.bzl", _java_library = "java_library", _java_test = "java_test")

_JAVA_BAZELRC_JAVACOPTS = [
    "-XepExcludedPaths:.*/external/.*",
    "-Werror",
    "-Aarez.warnings_as_errors=true",
    "-Aarez.persist.warnings_as_errors=true",
    "-Asting.warnings_as_errors=true",
    "-Areact4j.warnings_as_errors=true",
    "-Xlint:all,-processing,-serial,-options,-path,-classfile,-this-escape",
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
    "-Xep:BadInstanceof:ERROR",
    "-Xep:BadImport:ERROR",
    "-Xep:BareDotMetacharacter:ERROR",
    "-Xep:BigDecimalEquals:ERROR",
    "-Xep:BigDecimalLiteralDouble:ERROR",
    "-Xep:BoxedPrimitiveConstructor:ERROR",
    "-Xep:DeprecatedVariable:ERROR",
    "-Xep:ClassCanBeStatic:ERROR",
    "-Xep:DuplicateBranches:ERROR",
    "-Xep:EmptyBlockTag:ERROR",
    "-Xep:EmptyCatch:ERROR",
    "-Xep:EmptyTopLevelDeclaration:ERROR",
    "-Xep:Finalize:ERROR",
    "-Xep:InconsistentHashCode:ERROR",
    "-Xep:NotJavadoc:ERROR",
    "-Xep:NonOverridingEquals:ERROR",
    "-Xep:NullOptional:ERROR",
    "-Xep:NullablePrimitive:ERROR",
    "-Xep:NullablePrimitiveArray:ERROR",
    "-Xep:NullableTypeParameter:ERROR",
    "-Xep:NullableWildcard:ERROR",
    "-Xep:ParameterName:ERROR",
    "-Xep:EmptyIf:ERROR",
    "-Xep:ReturnAtTheEndOfVoidFunction:ERROR",
    "-Xep:ReturnFromVoid:ERROR",
    "-Xep:SelfAlwaysReturnsThis:ERROR",
    "-Xep:ToStringReturnsNull:ERROR",
    "-Xep:UnnecessaryMethodReference:ERROR",
    "-Xep:UnusedLabel:ERROR",
    "-Xep:UnsynchronizedOverridesSynchronized:ERROR",
    "-Xep:UnusedTypeParameter:ERROR",
    "-Xep:ClassName:ERROR",
    "-Xep:UseCorrectAssertInTests:ERROR",
    "-Xep:SystemExitOutsideMain:ERROR",
    "-Xep:MissingRuntimeRetention:ERROR",
    "-Xep:LongLiteralLowerCaseSuffix:ERROR",
    "-Xep:AnnotationPosition:ERROR",
    "-Xep:RedundantThrows:ERROR",
    "-Xep:RedundantOverride:ERROR",
    "-Xep:SunApi:ERROR",
    "-Xep:UnnecessarilyVisible:ERROR",
    "-Xep:UnnecessaryAnonymousClass:ERROR",
    "-Xep:UsingJsr305CheckReturnValue:ERROR",
    "-Xep:ConstantField:ERROR",
    "-Xep:EqualsMissingNullable:ERROR",
    "-Xep:FieldCanBeStatic:ERROR",
    "-Xep:ForEachIterable:ERROR",
    "-Xep:MissingBraces:ERROR",
    "-Xep:MixedArrayDimensions:ERROR",
    "-Xep:MultiVariableDeclaration:ERROR",
    "-Xep:MultipleTopLevelClasses:ERROR",
    "-Xep:PackageLocation:ERROR",
    "-Xep:PublicApiNamedStreamShouldReturnStream:ERROR",
    "-Xep:RemoveUnusedImports:ERROR",
    "-Xep:ReturnsNullCollection:ERROR",
    "-Xep:UnnecessaryBoxedAssignment:ERROR",
    "-Xep:VoidMissingNullable:ERROR",
    "-Xep:ReturnMissingNullable:ERROR",
    "-Xep:FieldCanBeLocal:ERROR",
    "-Xep:ParameterMissingNullable:ERROR",
    "-Xep:PrivateConstructorForUtilityClass:ERROR",
    "-Xep:FieldMissingNullable:ERROR",
    "-Xep:UnnecessaryDefaultInEnumSwitch:ERROR",
    "-Xep:UnnecessaryBoxedVariable:ERROR",
    "-Xep:FieldCanBeFinal:ERROR",
]

_SERVER_BAZELRC_JAVACOPTS = ["-Xep:Varifier:ERROR"]

_STRICT_JAVACOPTS = [
    "-XepDisableAllChecks",
    "-Werror",
    "-Xlint:all,-processing,-serial,-options,-deprecation,-this-escape",
    "-Xmaxerrs",
    "10000",
    "-Xmaxwarns",
    "10000",
]

def java_library(name, javacopts = [], **kwargs):
    _java_library(
        name = name,
        javacopts = javacopts + _STRICT_JAVACOPTS + _JAVA_BAZELRC_JAVACOPTS,
        **kwargs
    )

def java_server_library(name, javacopts = [], **kwargs):
    java_library(
        name = name,
        javacopts = javacopts + _SERVER_BAZELRC_JAVACOPTS,
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
        javacopts = javacopts + _STRICT_JAVACOPTS,
        jvm_flags = jvm_flags + [
            "-ea",
            "-Dbraincheck.environment=development",
            "-Darez.environment=development",
            "-Dreplicant.environment=development",
        ],
        runtime_deps = runtime_deps,
        **filtered_kwargs
    )
