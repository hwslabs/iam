# frozen_string_literal: true

require 'casbin-ruby/persist/adapter'

module Casbin
  module Persist
    module Adapters
      class StringAdapter < Persist::Adapter
        def initialize(policy)
          super()
          @policy = policy
        end

        def load_policy(model)
          load_policy_string(model)
        end

        def save_policy(model)
          raise 'Not implemented'
        end

        private

        attr_reader :policy

        def load_policy_string(model)
          rules = policy.split("\n")
          rules.each do |line|
            next if line.blank?
            load_policy_line(line.chomp.strip, model)
          end
        end
      end
    end
  end
end
