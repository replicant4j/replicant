require 'buildr/git_auto_version'
require 'buildr/jacoco'
require 'buildr/custom_pom'
require 'buildr/gpg'
require 'buildr/gwt'
require 'buildr/single_intermediate_layout'

GIN_DEPS = [:google_guice, :google_guice_assistedinject, :aopalliance, :gwt_gin, :javax_inject]

GWT_DEPS = [:gwt_user, :gwt_property_source] + GIN_DEPS
PROVIDED_DEPS = [:javax_jsr305, :javax_javaee, :glassfish_embedded]
COMPILE_DEPS = [:simple_session_filter, :field_filter]
TEST_INFRA_DEPS = [:mockito, :guiceyloops, :glassfish_embedded, :testng]
OPTIONAL_DEPS = GWT_DEPS, TEST_INFRA_DEPS
TEST_DEPS = TEST_INFRA_DEPS + [:jndikit]

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
    package(:jar).include("#{_(:source, :main, :java)}/*")
    package(:sources)
    package(:javadoc)

    iml.add_gwt_facet({'org.realityforge.replicant.ReplicantShared' => false},
                      :settings => {:compilerMaxHeapSize => '1024'},
                      :gwt_dev_artifact => :gwt_dev)
  end

  define 'server' do
    pom.provided_dependencies.concat PROVIDED_DEPS
    pom.optional_dependencies.concat OPTIONAL_DEPS

    compile.with PROVIDED_DEPS, COMPILE_DEPS, project('shared').package(:jar)

    package(:jar)
    package(:sources)
    package(:javadoc)

    test.using :testng
    test.compile.with TEST_DEPS
  end

  define 'client-common' do
    compile.with :javax_jsr305, :gwt_webpoller, project('shared').package(:jar)

    package(:jar).include("#{_(:source, :main, :java)}/*")
    package(:sources)
    package(:javadoc)

    test.using :testng
    test.compile.with TEST_DEPS

    iml.add_gwt_facet({'org.realityforge.replicant.ReplicantClientCommon' => false},
                      :settings => {:compilerMaxHeapSize => '1024'},
                      :gwt_dev_artifact => :gwt_dev)
  end

  define 'client-ee' do
    pom.provided_dependencies.concat PROVIDED_DEPS

    compile.with PROVIDED_DEPS,
                 project('client-common').package(:jar),
                 project('client-common').compile.dependencies

    package(:jar)
    package(:sources)
    package(:javadoc)

    test.using :testng
    test.compile.with TEST_DEPS
  end

  define 'client-gwt' do
    compile.with GWT_DEPS,
                 project('client-common').package(:jar),
                 project('client-common').compile.dependencies

    gwt(%w(org.realityforge.replicant.Replicant org.realityforge.replicant.ReplicantDev),
        :java_args => %w(-Xms512M -Xmx1024M -XX:PermSize=128M -XX:MaxPermSize=256M),
        :draft_compile => 'true') unless ENV['GWT'] == 'no'

    package(:jar).include("#{_(:source, :main, :java)}/*")
    package(:sources)
    package(:javadoc)

    test.using :testng
    test.compile.with TEST_DEPS

    iml.add_gwt_facet({'org.realityforge.replicant.Replicant' => false,
                       'org.realityforge.replicant.ReplicantDev' => false},
                      :settings => {:compilerMaxHeapSize => '1024'},
                      :gwt_dev_artifact => :gwt_dev)
  end

  define 'client-qa-support' do
    compile.with GWT_DEPS,
                 TEST_INFRA_DEPS,
                 project('client-common').package(:jar),
                 project('client-common').compile.dependencies

    package(:jar)
    package(:sources)
    package(:javadoc)

    test.using :testng
    test.compile.with TEST_DEPS
  end

  ipr.add_component_from_artifact(:idea_codestyle)

  iml.add_jruby_facet
end
