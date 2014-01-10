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

  compile.with PROVIDED_DEPS,
               :gwt_user,
               :jackson_core,
               :jackson_mapper

  test.with :jsonpath,
            :json,
            :json_smart

  package(:jar)
  package(:sources)

  test.using :testng
  test.compile.with :mockito
end
