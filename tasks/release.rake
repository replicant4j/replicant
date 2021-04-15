require 'buildr/release_tool.rb')

Buildr::ReleaseTool.define_release_task do |t|
  t.extract_version_from_changelog
  t.zapwhite
  t.ensure_git_clean
  t.cleanup_staging
  t.build
  t.patch_changelog('replicant4j/replicant')
  t.tag_project
  t.stage_release(:release_to => { :url => 'https://stocksoftware.jfrog.io/stocksoftware/staging', :username => ENV['STAGING_USERNAME'], :password => ENV['STAGING_PASSWORD'] })
  t.maven_central_publish
  t.patch_changelog_post_release
  t.push_changes
  t.github_release('replicant4j/replicant')
end
