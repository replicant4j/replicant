require 'buildr/release_tool'

Buildr::ReleaseTool.define_release_task do |t|
  t.extract_version_from_changelog
  t.zapwhite
  t.ensure_git_clean
  t.build
  t.patch_changelog('replicant4j/replicant')
  t.tag_project
  t.stage('MavenCentralPublish', 'Publish archive to Maven Central') do
    sh "bundle exec buildr upload_to_maven_central PRODUCT_VERSION=#{ENV['PRODUCT_VERSION']}#{ENV['TEST'].nil? ? '' : " TEST=#{ENV['TEST']}"}#{Buildr.application.options.trace ? ' --trace' : ''}"
  end
  t.patch_changelog_post_release
  t.push_changes
  t.github_release('replicant4j/replicant')
end
