# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with this
# work for additional information regarding copyright ownership.  The ASF
# licenses this file to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations under
# the License.

raise "Bug fixed in extension" if Buildr::VERSION > '1.4.9'

module Buildr
  module IntellijIdea
    class IdeaFile
      def components
        @components ||= []
      end

      def document
        if File.exist?(self.filename)
          doc = load_document(self.filename)
        else
          doc = base_document
          inject_components(doc, self.initial_components)
        end
        if self.template
          template_doc = load_document(self.template)
          REXML::XPath.each(template_doc, "//component") do |element|
            inject_component(doc, element)
          end
        end
        components = self.default_components.compact + self.components
        inject_components(doc, components)
        doc
      end
    end

    class IdeaProject
      def default_components
        [
          lambda { modules_component },
          vcs_component,
          artifacts_component,
          configurations_component,
          lambda { framework_detection_exclusion_component }
        ]
      end

      def framework_detection_exclusion_component
        create_component('FrameworkDetectionExcludesConfiguration') do |xml|
          xml.file :url => file_path(buildr_project._(:artifacts))
        end
      end
    end
  end
end
