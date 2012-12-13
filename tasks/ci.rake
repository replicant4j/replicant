def check_env_key(key, description)
  value = ENV[key]
  unless value
    $stderr.puts("Environment variable #{key} must be specified. #{key} should #{description}")
  end
  value
end

task "ci:setup" do
  ENV['TEST'] = 'all'
  repositories.release_to[:url] = check_env_key('UPLOAD_REPO', 'specify the url to the maven repository')
  repositories.release_to[:username] = check_env_key('UPLOAD_USER', 'specify the user to use to upload to the maven repository')
  repositories.release_to[:password] = check_env_key('UPLOAD_PASSWORD', 'specify the password to use to upload to the maven repository')
end

task "ci:package" => %w(ci:setup clean upload)
