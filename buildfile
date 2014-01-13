require 'buildr/git_auto_version'
require 'buildr/jacoco'

PROVIDED_DEPS = [:javax_inject, :javax_annotation, :javax_transaction, :javax_interceptor, :javax_persistence, :javax_naming, :javax_json]

desc "Replicant: Client-side state representation infrastructure"
define 'replicant' do
  project.group = 'org.realityforge.replicant'
  compile.options.source = '1.7'
  compile.options.target = '1.7'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache2_license
  pom.add_github_project("realityforge/replicant")
  pom.add_developer('realityforge', "Peter Donald")
  pom.provided_dependencies.concat PROVIDED_DEPS

  compile.with PROVIDED_DEPS,
               :gwt_user,
               :jackson_core,
               :jackson_mapper

  test.with :jsonpath,
            :json,
            :json_smart

  package(:jar).include("#{_(:source, :main, :java)}/*")
  package(:sources)
  package(:javadoc)

  test.using :testng
  test.compile.with :mockito
end
