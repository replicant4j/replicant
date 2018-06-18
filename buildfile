require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/gwt'
require 'buildr/jacoco'
require 'buildr/single_intermediate_layout'

REACT4J_DEPS = [:react4j_annotation, :react4j_core, :react4j_dom, :react4j_arez]

AREZ_DEPS = [:arez_annotations, :arez_core, :arez_component, :braincheck]

GWT_DEPS = [:elemental2_core, :elemental2_promise, :elemental2_dom, :elemental2_webstorage, :jsinterop_base, :jsinterop_annotations, :gwt_user]
PROVIDED_DEPS = [:javax_annotation, :javax_javaee, :glassfish_embedded]
KEYCLOAK_DEPS = [:simple_keycloak_service, :keycloak_adapter_core, :keycloak_adapter_spi, :keycloak_core, :keycloak_common]

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
    compile.with :javax_annotation

    package(:jar)
    package(:sources)
  end

  define 'server' do
    pom.provided_dependencies.concat PROVIDED_DEPS
    pom.dependency_filter = Proc.new do |dep|
      dep[:scope].to_s != 'test' &&
      project('shared').package(:jar) != dep[:artifact]
    end

    compile.with PROVIDED_DEPS,
                 KEYCLOAK_DEPS,
                 project('shared').package(:jar)

    package(:jar).enhance do |jar|
      jar.merge(project('shared').package(:jar))
    end
    package(:sources).enhance do |jar|
      jar.merge(project('shared').package(:jar, :classifier => :sources))
    end
    package(:javadoc)

    test.options[:properties] = TEST_OPTIONS
    test.options[:java_args] = ['-ea']

    test.using :testng
    test.compile.with :mockito,
                      :guiceyloops,
                      :glassfish_embedded,
                      :jndikit
  end

  define 'client' do
    pom.include_transitive_dependencies << artifact(:jetbrains_annotations)
    pom.include_transitive_dependencies << artifact(:elemental2_dom)
    pom.include_transitive_dependencies << artifact(:elemental2_webstorage)
    pom.include_transitive_dependencies << artifact(:react4j_arez)
    pom.include_transitive_dependencies << artifact(:arez_component)
    pom.include_transitive_dependencies << artifact(:gwt_webpoller)
    pom.include_transitive_dependencies << artifact(:gwt_user)
    pom.include_transitive_dependencies << artifact(:javax_javaee)

    pom.provided_dependencies.concat [:javax_javaee, :gwt_user]
    pom.dependency_filter = Proc.new do |dep|
      dep[:scope].to_s != 'test' &&
        !project('shared').compile.dependencies.include?(dep[:artifact]) &&
        project('shared').package(:jar) != dep[:artifact] &&
        (dep[:group].to_s != 'com.google.elemental2' || %w(elemental2-dom elemental2-webstorage).include?(dep[:id].to_s)) &&
        (dep[:group].to_s != 'org.realityforge.react4j' || %w(react4j-arez).include?(dep[:id].to_s)) &&
        (dep[:group].to_s != 'org.realityforge.arez' || %w(arez-component).include?(dep[:id].to_s)) &&
        dep[:group].to_s != 'com.google.jsinterop' &&
        dep[:group].to_s != 'org.realityforge.braincheck'
    end

    project.processorpath << :react4j_processor
    project.processorpath << :arez_processor

    compile.with project('shared').package(:jar),
                 project('shared').compile.dependencies,
                 :gwt_webpoller,
                 :jetbrains_annotations,
                 GWT_DEPS,
                 REACT4J_DEPS,
                 AREZ_DEPS,
                 # javax_javaee is provided so that JSON parsing can occur for JRE variant.
                 :javax_javaee

    gwt_enhance(project)

    package(:jar).enhance do |jar|
      jar.merge(project('shared').package(:jar))
    end
    package(:sources).enhance do |jar|
      jar.merge(project('shared').package(:jar, :classifier => :sources))
    end
    package(:javadoc)

    test.options[:properties] = TEST_OPTIONS
    test.options[:java_args] = ['-ea']

    test.using :testng
    test.compile.with :mockito,
                      :guiceyloops,
                      # javax_javaee is provided so that JSON parsing can occur for JRE variant.
                      :glassfish_embedded
  end

  ipr.add_default_testng_configuration(:jvm_args => '-ea -Dbraincheck.environment=development -Darez.environment=development -Dreplicant.environment=development')
  ipr.add_component_from_artifact(:idea_codestyle)

  iml.add_jruby_facet

  ([project] + projects).each do |p|
    p.enable_annotation_processor = false if p.processorpath.empty?
  end
  project('shared').task('upload').actions.clear
end
