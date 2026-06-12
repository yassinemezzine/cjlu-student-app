<#import "layout.ftl" as layout>
<@layout.adminPage title="Admin Login — CJLU" bodyClass="page-center">
    <div class="login-card card">
        <div class="brand">
            <span class="brand-mark">CJLU</span>
            <h1>Administration</h1>
        </div>
        <#if showError>
            <p class="alert alert-error">Invalid username or password</p>
        </#if>
        <form action="/admin/login" method="POST" class="stack">
            <label class="field">
                <span>Username</span>
                <input type="text" name="username" placeholder="Username" required autocomplete="username">
            </label>
            <label class="field">
                <span>Password</span>
                <input type="password" name="password" placeholder="Password" required autocomplete="current-password">
            </label>
            <button type="submit" class="btn btn-primary btn-block">Sign in</button>
        </form>
    </div>
</@layout.adminPage>
