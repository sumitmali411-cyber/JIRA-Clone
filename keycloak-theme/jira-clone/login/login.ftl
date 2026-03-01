<#import "template.ftl" as layout>
<@layout.registrationLayout
    displayMessage=!messagesPerField.existsError('username','password')
    displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??;
    section>

  <#if section = "header">
    <div class="jira-brand">
      <div class="jira-logo-circle">J</div>
      <span class="jira-app-name">JIRA Clone</span>
    </div>

  <#elseif section = "form">
    <#if realm.password>
      <form id="kc-form-login"
            onsubmit="login.disabled = true; return true;"
            action="${url.loginAction}"
            method="post">

        <div class="${properties.kcFormGroupClass!}">
          <label for="username" class="${properties.kcLabelClass!}">
            <#if !realm.loginWithEmailAllowed>
              ${msg("username")}
            <#elseif !realm.registrationEmailAsUsername>
              ${msg("usernameOrEmail")}
            <#else>
              ${msg("email")}
            </#if>
          </label>
          <input tabindex="1"
                 id="username"
                 class="${properties.kcInputClass!}"
                 name="username"
                 value="${(login.username!'')}"
                 type="text"
                 autofocus
                 autocomplete="off"
                 aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"/>
          <#if messagesPerField.existsError('username','password')>
            <span id="input-error" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
              ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
            </span>
          </#if>
        </div>

        <div class="${properties.kcFormGroupClass!}">
          <label for="password" class="${properties.kcLabelClass!}">${msg("password")}</label>
          <div class="${properties.kcInputGroup!}">
            <input tabindex="2"
                   id="password"
                   class="${properties.kcInputClass!}"
                   name="password"
                   type="password"
                   autocomplete="current-password"
                   aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>"/>
            <button class="${properties.kcFormPasswordVisibilityButtonClass!}"
                    type="button"
                    aria-label="${msg('showPassword')}"
                    aria-controls="password"
                    data-password-toggle
                    data-icon-show="${properties.kcFormPasswordVisibilityIconShow!}"
                    data-icon-hide="${properties.kcFormPasswordVisibilityIconHide!}">
              <i class="${properties.kcFormPasswordVisibilityIconShow!}" aria-hidden="true"></i>
            </button>
          </div>
        </div>

        <div class="${properties.kcFormGroupClass!} ${properties.kcFormSettingClass!}">
          <div id="kc-form-options">
            <#if realm.rememberMe && !usernameEditDisabled??>
              <div class="checkbox">
                <label>
                  <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"
                         <#if login.rememberMe??>checked</#if>>
                  ${msg("rememberMe")}
                </label>
              </div>
            </#if>
          </div>
          <div class="${properties.kcFormOptionsWrapperClass!}">
            <#if realm.resetPasswordAllowed>
              <span>
                <a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a>
              </span>
            </#if>
          </div>
        </div>

        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
          <input type="hidden" id="id-hidden-input" name="credentialId"
                 <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
          <input tabindex="4"
                 class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                 name="login" id="kc-login" type="submit" value="${msg('doLogIn')}"/>
        </div>
      </form>
    </#if>

  <#elseif section = "info">
    <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
      <div id="kc-registration-container">
        <div id="kc-registration">
          <span>
            ${msg("noAccount")}
            <a tabindex="6" href="${url.registrationUrl}">${msg("doRegister")}</a>
          </span>
        </div>
      </div>
    </#if>
  </#if>

</@layout.registrationLayout>
