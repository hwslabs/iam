require 'spec_helper'

# Unit tests for IamRuby::Helper::TokenUtil
describe IamRuby::Helper::TokenUtil do
  describe 'test an instance of TokenUtil' do
    it 'without token, should throw ArgumentError' do
      instance = IamRuby::Helper::TokenUtil.new
      expect{ instance }.to raise_error(ArgumentError)
    end

    it 'invalid token' do
      instance = IamRuby::Helper::TokenUtil.new('invalid_token')
      expect{ instance }.to raise_error(IamRuby::Helper::TokenUtil::TokenInvalidError)
    end

    it 'valid token, should work' do
      instance = IamRuby::Helper::TokenUtil.new('part1.part2.part3')
      expect{ instance }.to raise_error(IamRuby::Helper::TokenUtil::TokenInvalidError)
    end
  end
end
