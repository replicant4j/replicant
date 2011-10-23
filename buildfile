require 'buildr/java/emma'

desc "Replicant: Client-side state representation infrastructure"
define('replicant') do
  project.version = '0.2.1-dev'
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
               :javax_naming

  #iml.add_gwt_facet

  package(:jar)
  package(:sources)

  test.using :testng

  emma.include 'org.realityforge.*'
end
