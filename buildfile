require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/gwt'
require 'buildr/jacoco'
require 'buildr/single_intermediate_layout'

REACT4J_DEPS = [:react4j_annotation, :react4j_core, :react4j_dom, :react4j_arez]

AREZ_DEPS = [
  :arez_annotations, :arez_core, :arez_processor, :arez_component, :braincheck, :anodoc, :jetbrains_annotations
]

GWT_DEPS = [:elemental2_core, :elemental2_promise, :elemental2_dom, :elemental2_webstorage, :jsinterop_base, :jsinterop_base_sources, :jsinterop_annotations, :jsinterop_annotations_sources, :gwt_user]
PROVIDED_DEPS = [:javax_jsr305, :javax_javaee, :glassfish_embedded]
KEYCLOAK_DEPS = [:simple_keycloak_service, :keycloak_adapter_core, :keycloak_adapter_spi, :keycloak_core, :keycloak_common]
COMPILE_DEPS = KEYCLOAK_DEPS
TEST_INFRA_DEPS = [:mockito, :guiceyloops, :glassfish_embedded, :testng]
OPTIONAL_DEPS = GWT_DEPS, TEST_INFRA_DEPS
TEST_DEPS = TEST_INFRA_DEPS + [:jndikit]

# JDK options passed to test environment. Essentially turns assertions on.
TEST_OPTIONS =
  {
    'braincheck.environment' => 'development',
    'arez.environment' => 'development',
    'replicant.environment' => 'development'
  }

desc 'Replicant: Client-side state representation infrastructure'
define 'replicant' do
  project.group = 'org.realityforge.replicant'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/replicant')
  pom.add_developer('realityforge', 'Peter Donald')

  define 'shared' do
    compile.with :javax_jsr305

    gwt_enhance(project)

    package(:jar)
    package(:sources)
    package(:javadoc)
  end

  define 'server' do
    pom.provided_dependencies.concat PROVIDED_DEPS
    pom.optional_dependencies.concat OPTIONAL_DEPS

    compile.with PROVIDED_DEPS, COMPILE_DEPS, project('shared').package(:jar)

    package(:jar)
    package(:sources)
    package(:javadoc)

    test.options[:properties] = TEST_OPTIONS
    test.options[:java_args] = ['-ea']

    test.using :testng
    test.compile.with TEST_DEPS
  end

  define 'client' do
    pom.provided_dependencies.concat [:javax_javaee, :gwt_user]

    project.processorpath << :react4j_processor
    project.processorpath << :arez_processor

    compile.with project('shared').package(:jar),
                 project('shared').compile.dependencies,
                 :gwt_webpoller,
                 GWT_DEPS,
                 REACT4J_DEPS,
                 AREZ_DEPS,
                 # javax_javaee is provided so that JSON parsing can occur for JRE variant.
                 :javax_javaee

    gwt_enhance(project)

    package(:jar)
    package(:sources)
    package(:javadoc)

    test.options[:properties] = TEST_OPTIONS
    test.options[:java_args] = ['-ea']

    test.using :testng
    test.compile.with TEST_DEPS
  end

  ipr.add_default_testng_configuration(:jvm_args => '-ea -Dbraincheck.environment=development -Darez.environment=development -Dreplicant.environment=development')
  ipr.add_component_from_artifact(:idea_codestyle)

  iml.add_jruby_facet

  ([project] + projects).each do |p|
    p.enable_annotation_processor = false if p.processorpath.empty?
  end
end
