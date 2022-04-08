# -*- encoding: utf-8 -*-

=begin
#Hypto IAM

#APIs for Hypto IAM Service.

The version of the OpenAPI document: 1.0.0
Contact: engineering@hypto.in
Generated by: https://openapi-generator.tech
OpenAPI Generator version: 5.4.0

=end

$:.push File.expand_path("../lib", __FILE__)
require "iam-ruby/version"

Gem::Specification.new do |s|
  s.name        = "iam-ruby"
  s.version     = IamRuby::VERSION
  s.platform    = Gem::Platform::RUBY
  s.authors     = ["OpenAPI-Generator"]
  s.email       = ["engineering@hypto.in"]
  s.homepage    = "https://openapi-generator.tech"
  s.summary     = "Hypto IAM Ruby Gem"
  s.description = "IAM Ruby Client"
  s.license     = "Unlicense"
  s.required_ruby_version = ">= 2.4"

  s.add_runtime_dependency 'typhoeus', '~> 1.0', '>= 1.0.1'

  s.add_development_dependency 'rspec', '~> 3.6', '>= 3.6.0'

  s.files         = `find *`.split("\n").uniq.sort.select { |f| !f.empty? }
  s.test_files    = `find spec/*`.split("\n")
  s.executables   = []
  s.require_paths = ["lib"]
end
