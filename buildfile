require 'buildr/git_auto_version'
require 'buildr/jacoco'
require 'buildr/custom_pom'
require 'buildr/gpg'

GIN_DEPS = [:google_guice, :google_guice_assistedinject, :aopalliance, :gwt_gin]

PROVIDED_DEPS = [:javax_inject, :javax_annotation, :javax_transaction, :javax_interceptor, :javax_persistence, :javax_naming, :javax_json, :gwt_user]
OPTIONAL_DEPS = GIN_DEPS

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

  compile.with PROVIDED_DEPS, OPTIONAL_DEPS, :simple_session_filter, :gwt_property_source

  test.using :testng
  test.compile.with :mockito, :jndikit, :guiceyloops, :json

  package(:jar).include("#{_(:source, :main, :java)}/*")
  package(:sources)
  package(:javadoc)
end
