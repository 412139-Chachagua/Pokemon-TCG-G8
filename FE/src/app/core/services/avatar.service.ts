import { Injectable } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';

@Injectable({ providedIn: 'root' })
export class AvatarService {
  private readonly uploadsUrl = `${API_BASE_URL}/uploads/`;

  resolve(relativePath: string | null | undefined): string | null {
    if (!relativePath) return null;
    return this.uploadsUrl + relativePath;
  }
}
