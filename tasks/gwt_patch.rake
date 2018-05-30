module JavadocCleanerExtension
  include Extension

  after_define(:doc) do |project|
    project.doc.classpath.delete_if {|f| f.to_s =~ /.*\/org\/realityforge\/gwt\/webpoller\/gwt-webpoller\/.*/}
  end
end

class Buildr::Project
  include JavadocCleanerExtension
end
