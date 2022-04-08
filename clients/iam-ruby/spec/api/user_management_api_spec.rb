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

# Unit tests for IamRuby::UserManagementApi
# Automatically generated by openapi-generator (https://openapi-generator.tech)
# Please update as you see appropriate
describe 'UserManagementApi' do
  before do
    # run before each test
    @api_instance = IamRuby::UserManagementApi.new
  end

  after do
    # run after each test
  end

  describe 'test an instance of UserManagementApi' do
    it 'should create an instance of UserManagementApi' do
      expect(@api_instance).to be_instance_of(IamRuby::UserManagementApi)
    end
  end

  # unit tests for create_user
  # Create a user
  # User is an entity which represent a person who is part of the organization or account. This user entity can be created either through user name, password or the user can be federated through an identity provider like Google, Facebook or any SAML 2.0, OIDC identity provider. This is a sign-up api to create a new user in an organization.
  # @param organization_id 
  # @param create_user_request Payload to create user
  # @param [Hash] opts the optional parameters
  # @return [User]
  describe 'create_user test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for delete_user
  # Delete a User
  # Delete a User
  # @param user_name 
  # @param organization_id 
  # @param [Hash] opts the optional parameters
  # @return [BaseSuccessResponse]
  describe 'delete_user test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for get_user
  # Gets a user entity associated with the organization
  # Get a User
  # @param user_name 
  # @param organization_id 
  # @param [Hash] opts the optional parameters
  # @return [User]
  describe 'get_user test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for list_users
  # List users
  # List users associated with the organization. This is a pagniated api which returns the list of users with a next page token.
  # @param organization_id 
  # @param [Hash] opts the optional parameters
  # @option opts [String] :next_token 
  # @option opts [String] :page_size 
  # @return [UserPaginatedResponse]
  describe 'list_users test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

  # unit tests for update_user
  # Update a User
  # Update a User
  # @param user_name 
  # @param organization_id 
  # @param update_user_request Payload to update user
  # @param [Hash] opts the optional parameters
  # @return [User]
  describe 'update_user test' do
    it 'should work' do
      # assertion here. ref: https://www.relishapp.com/rspec/rspec-expectations/docs/built-in-matchers
    end
  end

end
