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

# Unit tests for IamRuby::PolicyManagementApi
# Automatically generated by openapi-generator (https://openapi-generator.tech)
# Please update as you see appropriate
describe 'PolicyManagementApi' do
  before do
    # run before each test
    @api_instance = IamRuby::PolicyManagementApi.new
  end

  after do
    # run after each test
  end

  describe 'test an instance of PolicyManagementApi' do
    it 'should create an instance of PolicyManagementApi' do
      expect(@api_instance).to be_instance_of(IamRuby::PolicyManagementApi)
    end
  end

  # unit tests for create_policy
  # Create a policy
  # Create a policy
  # @param organization_id 
  # @param create_policy_request Payload to create policy
  # @param [Hash] opts the optional parameters
  # @return [Policy]
  describe 'create_policy test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for delete_policy
  # Delete a policy
  # Delete a policy
  # @param organization_id 
  # @param policy_name 
  # @param [Hash] opts the optional parameters
  # @return [BaseSuccessResponse]
  describe 'delete_policy test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for get_policy
  # Get a policy
  # Get a policy
  # @param organization_id 
  # @param policy_name 
  # @param [Hash] opts the optional parameters
  # @return [Policy]
  describe 'get_policy test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for list_policies
  # List policies
  # List policies
  # @param organization_id 
  # @param [Hash] opts the optional parameters
  # @option opts [String] :next_token 
  # @option opts [String] :page_size 
  # @option opts [String] :sort_order 
  # @return [PolicyPaginatedResponse]
  describe 'list_policies test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for update_policy
  # Update a policy
  # Update a policy
  # @param organization_id 
  # @param policy_name 
  # @param update_policy_request Payload to update policy
  # @param [Hash] opts the optional parameters
  # @return [Policy]
  describe 'update_policy test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

end
