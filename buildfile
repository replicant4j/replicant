require 'buildr/git_auto_version'
require 'buildr/java/emma'

desc "Replicant: Client-side state representation infrastructure"
define('replicant') do
  project.group = 'org.realityforge'
  compile.options.source = '1.6'
  compile.options.target = '1.6'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  compile.with :gwt_user,
               :javax_inject,
               :javax_annotation,
               :javax_transaction,
               :javax_interceptor,
               :javax_persistence,
               :javax_naming,
               :json,
               :jackson_core,
               :jackson_mapper

  test.with :jsonpath,
            :json_smart

  package(:jar)
  package(:sources)

  test.using :testng
  test.compile.with :mockito

  emma.include 'org.realityforge.*'
end
