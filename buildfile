require 'buildr/git_auto_version'
require 'buildr/gpg'
require 'buildr/gwt'
require 'buildr/jacoco'
require 'buildr/single_intermediate_layout'

GWT_DEPS = [:elemental2_core, :elemental2_promise, :elemental2_dom, :elemental2_webstorage, :jsinterop_base, :jsinterop_annotations, :gwt_user]
PROVIDED_DEPS = [:javax_annotation, :javax_javaee, :glassfish_embedded]
KEYCLOAK_DEPS = [:simple_keycloak_service, :keycloak_adapter_core, :keycloak_adapter_spi, :keycloak_core, :keycloak_common]

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
  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all,-processing,-serial'
  project.compile.options.warnings = true
  project.compile.options.other = %w(-Werror -Xmaxerrs 10000 -Xmaxwarns 10000)

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
                 :graphql_java,
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
    pom.include_transitive_dependencies << artifact(:jetbrains_annotations)
    pom.include_transitive_dependencies << artifact(:elemental2_dom)
    pom.include_transitive_dependencies << artifact(:elemental2_webstorage)
    pom.include_transitive_dependencies << artifact(:react4j_core)
    pom.include_transitive_dependencies << artifact(:arez_core)
    pom.include_transitive_dependencies << artifact(:gwt_user)
    pom.include_transitive_dependencies << artifact(:zemeckis)

    pom.provided_dependencies.concat [:gwt_user]
    pom.dependency_filter = Proc.new do |dep|
      dep[:scope].to_s != 'test' &&
        !project('shared').compile.dependencies.include?(dep[:artifact]) &&
        project('shared').package(:jar) != dep[:artifact] &&
        (dep[:group].to_s != 'org.realityforge.com.google.elemental2' || %w(elemental2-dom elemental2-webstorage).include?(dep[:id].to_s)) &&
        (dep[:group].to_s != 'org.realityforge.react4j' || %w(react4j-core).include?(dep[:id].to_s)) &&
        dep[:group].to_s != 'com.google.jsinterop' &&
        dep[:group].to_s != 'org.realityforge.grim' &&
        dep[:group].to_s != 'org.realityforge.braincheck'
    end

    project.processorpath << :react4j_processor
    project.processorpath << :arez_processor
    project.processorpath << artifacts(:grim_processor, :javax_json)

    compile.with project('shared').package(:jar),
                 project('shared').compile.dependencies,
                 :jetbrains_annotations,
                 GWT_DEPS,
                 :react4j_core,
                 :gwt_keycloak,
                 :braincheck,
                 :grim_annotations,
                 :zemeckis,
                 :arez_core

    gwt_enhance(project)

    package(:jar).enhance do |jar|
      jar.merge(project('shared').package(:jar))
    end
    package(:sources).enhance do |jar|
      jar.merge(project('shared').package(:jar, :classifier => :sources))
    end
    package(:javadoc)

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
                      :arez_testng,
                      :guiceyloops,
                      # javax_javaee is provided so that JSON parsing can occur for JRE variant.
                      :glassfish_embedded
  end

  iml.excluded_directories << project._('tmp')

  ipr.add_default_testng_configuration(:jvm_args => '-ea -Dbraincheck.environment=development -Darez.environment=development -Dreplicant.environment=development')
  ipr.add_component_from_artifact(:idea_codestyle)

  ipr.add_component('JavacSettings') do |xml|
    xml.option(:name => 'ADDITIONAL_OPTIONS_STRING', :value => '-Xlint:all,-processing,-serial -Werror -Xmaxerrs 10000 -Xmaxwarns 10000')
  end

  ([project] + projects).each do |p|
    p.enable_annotation_processor = false if p.processorpath.empty?
  end
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

  ipr.add_component('JavaProjectCodeInsightSettings') do |xml|
    xml.tag!('excluded-names') do
      xml << '<name>com.sun.istack.internal.NotNull</name>'
      xml << '<name>com.sun.istack.internal.Nullable</name>'
      xml << '<name>org.jetbrains.annotations.Nullable</name>'
      xml << '<name>org.jetbrains.annotations.NotNull</name>'
      xml << '<name>org.testng.AssertJUnit</name>'
    end
  end
  ipr.add_component('NullableNotNullManager') do |component|
    component.option :name => 'myDefaultNullable', :value => 'javax.annotation.Nullable'
    component.option :name => 'myDefaultNotNull', :value => 'javax.annotation.Nonnull'
    component.option :name => 'myNullables' do |option|
      option.value do |value|
        value.list :size => '2' do |list|
          list.item :index => '0', :class => 'java.lang.String', :itemvalue => 'org.jetbrains.annotations.Nullable'
          list.item :index => '1', :class => 'java.lang.String', :itemvalue => 'javax.annotation.Nullable'
        end
      end
    end
    component.option :name => 'myNotNulls' do |option|
      option.value do |value|
        value.list :size => '2' do |list|
          list.item :index => '0', :class => 'java.lang.String', :itemvalue => 'org.jetbrains.annotations.NotNull'
          list.item :index => '1', :class => 'java.lang.String', :itemvalue => 'javax.annotation.Nonnull'
        end
      end
    end
  end
end
