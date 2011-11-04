require 'buildr/java/emma'

desc "Replicant: Client-side state representation infrastructure"
define('replicant') do
  project.version = `git describe --tags`.strip
  project.group = 'org.realityforge'
  compile.options.source = '1.6'
  compile.options.target = '1.6'
  compile.options.lint = 'all'

  compile.with :gwt_user,
               :javax_inject,
               :javax_annotation,
               :javax_transaction,
               :javax_interceptor,
               :javax_persistence,
               :javax_naming,
               :json

  test.with :jsonpath,
            :json_smart

  #iml.add_gwt_facet

  package(:jar)
  package(:sources)

  test.using :testng
  test.compile.with :mockito

  emma.include 'org.realityforge.*'
end
