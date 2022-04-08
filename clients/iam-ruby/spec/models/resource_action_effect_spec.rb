=begin
#Hypto IAM

#APIs for Hypto IAM Service.

The version of the OpenAPI document: 1.0.0
Contact: engineering@hypto.in
Generated by: https://openapi-generator.tech
OpenAPI Generator version: 5.4.0

=end

require 'spec_helper'
require 'json'
require 'date'

# Unit tests for IamRuby::ResourceActionEffect
# Automatically generated by openapi-generator (https://openapi-generator.tech)
# Please update as you see appropriate
describe IamRuby::ResourceActionEffect do
  let(:instance) { IamRuby::ResourceActionEffect.new }

  describe 'test an instance of ResourceActionEffect' do
    it 'should create an instance of ResourceActionEffect' do
      expect(instance).to be_instance_of(IamRuby::ResourceActionEffect)
    end
  end
  describe 'test attribute "resource"' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  describe 'test attribute "action"' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  describe 'test attribute "effect"' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
      # validator = Petstore::EnumTest::EnumAttributeValidator.new('String', ["allow", "deny"])
      # validator.allowable_values.each do |value|
      #   expect { instance.effect = value }.not_to raise_error
      # end
    end
  end

end
