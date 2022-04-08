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

# Unit tests for IamRuby::UpdateCredentialRequest
# Automatically generated by openapi-generator (https://openapi-generator.tech)
# Please update as you see appropriate
describe IamRuby::UpdateCredentialRequest do
  let(:instance) { IamRuby::UpdateCredentialRequest.new }

  describe 'test an instance of UpdateCredentialRequest' do
    it 'should create an instance of UpdateCredentialRequest' do
      expect(instance).to be_instance_of(IamRuby::UpdateCredentialRequest)
    end
  end
  describe 'test attribute "valid_until"' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  describe 'test attribute "status"' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
      # validator = Petstore::EnumTest::EnumAttributeValidator.new('String', ["active", "inactive"])
      # validator.allowable_values.each do |value|
      #   expect { instance.status = value }.not_to raise_error
      # end
    end
  end

end
