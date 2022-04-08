require 'zlib'
require 'casbin-ruby'


module IamRuby
  module Helper
    class TokenUtil
    
    class TokenInvalidError < StandardError
      def initialize(msg = 'Invalid Token')
        super
      end
    end

    attr_reader :usr, :entitlements

    def initialize(token)
      token_split = token.to_s.split('.')
      raise TokenInvalidError if token_split.size != 3

      decoded_body = Base64.urlsafe_decode64(token_split[1].to_s)
      decompressed_body = Zlib::GzipReader.new(StringIO.new(decoded_body)).read
      payload = JSON.parse(decompressed_body)

      @usr = payload['usr']
      @entitlements = payload['entitlements']
      @enforcer = Casbin::Enforcer.new("./casbin_model.conf", 
        Casbin::Persist::Adapters::StringAdapter.new(payload['entitlements']))
    end

    def has_permission? resource, action
      @enforcer.enforce(@usr, resource, action)
    end
  end
end
