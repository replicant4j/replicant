require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/gwt'
require 'buildr/single_intermediate_layout'

Buildr::MavenCentral.define_publish_tasks(:profile_name => 'org.realityforge', :username => 'realityforge')


FORMATTER_JDK_EXPORTS =
  %w(
    --add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
    --add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
  )
FORMATTER_JAVAC_EXPORTS = FORMATTER_JDK_EXPORTS.map { |option| "-J#{option}" }

PROVIDED_DEPS = [
  :jspecify,
  :javax_annotation,
  :jakarta_annotation_api,
  :jakarta_enterprise_cdi_api,
  :javax_enterprise_concurrent_api,
  :jakarta_inject,
  :jakarta_interceptor_api,
  :javax_persistence_api,
  :jakarta_transaction_api,
  :jakarta_websocket_api,
  :jetbrains_annotations,
  :javax_json
]

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
  compile.options.source = '17'
  compile.options.target = '17'
  compile.options.lint = 'all,-processing,-serial,-options,-deprecation'
  project.compile.options.warnings = true
  project.compile.options.other = %w(-Werror -Xmaxerrs 10000 -Xmaxwarns 10000) + FORMATTER_JAVAC_EXPORTS

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('replicant4j/replicant')
  pom.add_developer('realityforge', 'Peter Donald')

  define 'shared' do
    compile.with :jspecify

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
                      :byte_buddy,
                      :objenesis,
                      :jndikit
  end

  define 'client' do
    client_deps =
      artifacts(:jetbrains_annotations,
                :akasha,
                :react4j_core,
                :arez_core,
                :gwt_user,
                :grim_annotations,
                :zemeckis,
                :jspecify,
                :jsinterop_base,
                :jsinterop_annotations,
                :braincheck_core)
    pom.include_transitive_dependencies << client_deps

    pom.provided_dependencies.concat [:gwt_user]
    pom.dependency_filter = Proc.new {|dep| dep[:scope].to_s != 'test' && client_deps.include?(dep[:artifact]) }

    # Arez 0.254 generated sources still import javax.annotation nullness types.
    compile.with client_deps + [:javax_annotation, project('shared').package(:jar)]

    compile.options[:processor_path] << [:arez_processor, :react4j_processor, :grim_processor, :javax_json]
    compile.options.other = FORMATTER_JAVAC_EXPORTS

    package(:jar).enhance do |jar|
      jar.merge(project('shared').package(:jar))
    end
    package(:sources).enhance do |jar|
      jar.merge(project('shared').package(:jar, :classifier => :sources))
      jar.include(project._(:target, :generated, 'processors/main/java'))
    end
    package(:javadoc)

    gwt_enhance(project)

    test.options[:properties] = {
      'braincheck.environment' => 'development',
      'arez.environment' => 'development',
      'replicant.environment' => 'development',
      'replicant.check_diagnostic_messages' => 'true',
      'replicant.diagnostic_messages_file' => _('src/test/java/replicant/diagnostic_messages.json')
    }
    test.options[:java_args] = ['-ea']

    test.using :testng
    test.compile.with :mockito,
                      :byte_buddy,
                      :objenesis,
                      :braincheck_testng,
                      :arez_testng,
                      :javax_json
  end

  iml.excluded_directories << project._('tmp')

  ipr.add_default_testng_configuration(:jvm_args => '-ea -Dbraincheck.environment=development -Darez.environment=development -Dreplicant.environment=development')

  project('shared').task('upload').actions.clear

  ipr.add_testng_configuration('client',
                               :module => 'client',
                               :jvm_args => '-ea -Dbraincheck.environment=development -Darez.environment=development -Dreplicant.environment=development -Dreplicant.output_fixture_data=true -Dreplicant.check_diagnostic_messages=false -Dreplicant.diagnostic_messages_file=src/test/java/replicant/diagnostic_messages.json')
  ipr.add_testng_configuration('client - update invariant messages',
                               :module => 'client',
                               :jvm_args => '-ea -Dbraincheck.environment=development -Darez.environment=development -Dreplicant.environment=development -Dreplicant.output_fixture_data=true -Dreplicant.check_diagnostic_messages=true -Dreplicant.diagnostic_messages_file=src/test/java/replicant/diagnostic_messages.json')
  ipr.add_testng_configuration('server',
                               :module => 'server',
                               :jvm_args => '-ea')

  ipr.add_code_insight_settings
  ipr.add_nullable_manager
  ipr.add_javac_settings('-Xlint:all,-processing,-serial,-options,-deprecation -Werror -Xmaxerrs 10000 -Xmaxwarns 10000')
end
