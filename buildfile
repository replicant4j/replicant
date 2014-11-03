require 'buildr/git_auto_version'
require 'buildr/jacoco'
require 'buildr/custom_pom'
require 'buildr/gpg'
require 'buildr/gwt'

GIN_DEPS = [:google_guice, :google_guice_assistedinject, :aopalliance, :gwt_gin]

GWT_DEPS = [:gwt_user, :gwt_property_source, :gwt_webpoller] + GIN_DEPS
PROVIDED_DEPS = [:javax_annotation, :javax_javaee]
COMPILE_DEPS = [:simple_session_filter]
TEST_INFRA_DEPS = [:mockito, :guiceyloops]
OPTIONAL_DEPS = GWT_DEPS, TEST_INFRA_DEPS
TEST_DEPS = TEST_INFRA_DEPS + [:jndikit, :javax_json_impl]

desc 'Replicant: Client-side state representation infrastructure'
define 'replicant' do
  project.group = 'org.realityforge.replicant'
  compile.options.source = '1.7'
  compile.options.target = '1.7'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/replicant')
  pom.add_developer('realityforge', 'Peter Donald')
  pom.provided_dependencies.concat PROVIDED_DEPS
  pom.optional_dependencies.concat OPTIONAL_DEPS

  compile.with PROVIDED_DEPS, COMPILE_DEPS, OPTIONAL_DEPS

  gwt(['org.realityforge.replicant.Replicant'],
                :java_args => ['-Xms512M', '-Xmx1024M', '-XX:PermSize=128M', '-XX:MaxPermSize=256M'],
                :draft_compile => (ENV['FAST_GWT'] == 'true'),
                :dependencies => [:javax_validation, :javax_validation_sources] + project.compile.dependencies)


  test.using :testng
  test.compile.with TEST_DEPS

  package(:jar).include("#{_(:source, :main, :java)}/*")
  package(:sources)
  package(:javadoc)

  iml.add_jruby_facet
  iml.add_gwt_facet({'org.realityforge.replicant.Replicant' => true},
                    :settings => {:compilerMaxHeapSize => '1024'},
                    :gwt_dev_artifact => :gwt_dev)
end
