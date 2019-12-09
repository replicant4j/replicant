require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/gwt'
require 'buildr/jacoco'
require 'buildr/single_intermediate_layout'

GIN_DEPS = [:google_guice, :google_guice_assistedinject, :aopalliance, :gwt_gin, :javax_inject]

GWT_DEPS = [:gwt_user] + GIN_DEPS
PROVIDED_DEPS = [:javax_jsr305, :javax_javaee, :glassfish_embedded]
KEYCLOAK_DEPS = [:simple_keycloak_service, :keycloak_adapter_core, :keycloak_adapter_spi, :keycloak_core, :keycloak_common]
COMPILE_DEPS = KEYCLOAK_DEPS
TEST_INFRA_DEPS = [:mockito, :guiceyloops, :glassfish_embedded, :testng]

desc 'Replicant: Client-side state representation infrastructure'
define 'replicant' do
  project.group = 'org.realityforge.replicant'
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('replicant4j/replicant')
  pom.add_developer('realityforge', 'Peter Donald')

  define 'shared' do
    compile.with :javax_annotation

    package(:jar).tap do |j|
      j.include("#{project._(:source, :main, :java)}/*")
    end
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

    test.options[:java_args] = ['-ea']

    test.using :testng

    test.compile.with :mockito,
                      :guiceyloops,
                      :glassfish_embedded,
                      :jndikit
  end

  define 'client' do
    compile.with GWT_DEPS,
                 :javax_jsr305,
                 :gwt_webpoller,
                 project('shared').package(:jar),
                 :mockito,
                 :guiceyloops

    package(:jar).enhance do |jar|
      jar.include("#{_(:source, :main, :java)}/*")
      jar.include("#{_(:source, :main, :super)}/*")
      jar.merge(project('shared').package(:jar))
    end
    package(:sources).enhance do |jar|
      jar.include("#{_(:source, :main, :super)}/*")
      jar.merge(project('shared').package(:jar, :classifier => :sources))
    end
    package(:javadoc)

    gwt(%w(org.realityforge.replicant.Replicant org.realityforge.replicant.ReplicantDev),
        :java_args => %w(-Xms512M -Xmx1024M),
        :draft_compile => 'true') unless ENV['GWT'] == 'no'

    test.using :testng
    test.compile.with :mockito,
                      :guiceyloops,
                      # javax_javaee is provided so that JSON parsing can occur for JRE variant.
                      :glassfish_embedded

    iml.add_gwt_facet({'org.realityforge.replicant.Replicant' => false,
                       'org.realityforge.replicant.ReplicantDev' => false},
                      :settings => {:compilerMaxHeapSize => '1024'},
                      :gwt_dev_artifact => :gwt_dev)

    # Not sure what in this project breakes jacoco
    jacoco.enabled = false
  end

  ipr.add_component_from_artifact(:idea_codestyle)

  iml.add_jruby_facet

  ([project] + projects).each do |p|
    p.enable_annotation_processor = false if p.processorpath.empty?
  end
  project('shared').task('upload').actions.clear
end
