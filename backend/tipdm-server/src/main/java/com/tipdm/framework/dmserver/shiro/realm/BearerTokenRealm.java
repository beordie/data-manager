package com.tipdm.framework.dmserver.shiro.realm;

import com.tipdm.framework.common.token.TokenManager;
import com.tipdm.framework.common.token.model.TokenModel;
import com.tipdm.framework.dmserver.shiro.token.BearerAuthenticationToken;
import com.tipdm.framework.service.dmserver.DataSchemaService;
import com.tipdm.framework.service.dmserver.DataTableService;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by TipDM on 2016/12/6.
 * E-mail:devp@tipdm.com
 */
public class BearerTokenRealm extends AuthorizingRealm /* AuthorizingRealm | AuthenticatingRealm*/ {

    @Autowired
    private TokenManager tokenManager;

    private DataSchemaService dataSchemaService;

    public void setTokenManager(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public void setDataSchemaService(DataSchemaService dataSchemaService) {
        this.dataSchemaService = dataSchemaService;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        //仅支持StatelessToken类型的Token
        return token instanceof BearerAuthenticationToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        //根据用户名查找角色，请根据需求实现
        String token = (String) principals.getPrimaryPrincipal();
        TokenModel tokenModel = tokenManager.getPermissions(token);

        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        if (null != tokenModel) {
            authorizationInfo.setRoles(tokenModel.getRoles());
            authorizationInfo.setStringPermissions(tokenModel.getPermissions());
        }
        return authorizationInfo;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        BearerAuthenticationToken authToken = (BearerAuthenticationToken) token;
        String accessToken = (String) authToken.getCredentials();
        String principal = (String) authToken.getPrincipal();

        //检查token是否有效
        TokenModel tokenModel = tokenManager.getPermissions(accessToken);
        //无效的token或已过期
        if (null == tokenModel) {
            throw new AuthenticationException("accessToken已失效，请重新获取！");
        }

        try {
            //不建议在生产环境下调用
            dataSchemaService.createSchema(tokenModel.getUsername());
        } catch (Exception ex){

        }
        return new SimpleAuthenticationInfo(
                accessToken,
                principal,
                getName());
    }


    @Override
    public void clearCachedAuthorizationInfo(PrincipalCollection principals) {
        super.clearCachedAuthorizationInfo(principals);
    }

    @Override
    public void clearCachedAuthenticationInfo(PrincipalCollection principals) {
        super.clearCachedAuthenticationInfo(principals);
    }

    @Override
    public void clearCache(PrincipalCollection principals) {
        super.clearCache(principals);
    }

    public void clearAllCachedAuthorizationInfo() {
        getAuthorizationCache().clear();
    }

    public void clearAllCachedAuthenticationInfo() {
        getAuthenticationCache().clear();
    }

    public void clearAllCache() {
        clearAllCachedAuthenticationInfo();
        clearAllCachedAuthorizationInfo();
    }
}
