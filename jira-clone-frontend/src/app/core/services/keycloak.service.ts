import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private keycloak = new Keycloak({
    url: environment.keycloak.url,
    realm: environment.keycloak.realm,
    clientId: environment.keycloak.clientId
  });

  init(): Promise<void> {
    return this.keycloak
      .init({ onLoad: 'login-required', checkLoginIframe: false })
      .then(() => undefined);
  }

  getToken(): Promise<string> {
    return this.keycloak.updateToken(30).then(() => this.keycloak.token!);
  }

  logout(): void {
    this.keycloak.logout({ redirectUri: 'http://localhost:4200' });
  }

  getUsername(): string {
    return this.keycloak.tokenParsed?.['preferred_username'] ?? '';
  }

  getUserFullName(): string {
    return this.keycloak.tokenParsed?.['name'] ?? this.getUsername();
  }

  getInitials(): string {
    const name = this.getUserFullName();
    return name
      .split(' ')
      .map(n => n[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }
}
