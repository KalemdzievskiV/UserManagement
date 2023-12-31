import { HttpClient, HttpErrorResponse, HttpEvent, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from 'environments/environment';
import { CustomHttpRespone } from '../model/custom-http-response';
import { User } from '../model/user';


@Injectable({
  providedIn: 'root'
})
export class UserService {

  private host = environment.apiUrl;

  constructor(private http: HttpClient) { }

  public getUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.host}/user/list`);
  }

  public addUser(fromData: FormData): Observable<User>{
    return this.http.post<User>(`${this.host}/user/add`, fromData);
  }

  public updateUser(fromData: FormData): Observable<User>{
    return this.http.post<User>(`${this.host}/user/update`, fromData);
  }

  public resetPassword(email: string): Observable<CustomHttpRespone>{
    return this.http.get<CustomHttpRespone>(`${this.host}/user/resetPassword/${email}`);
  }

  public updateProfileImage(fromData: FormData): Observable<HttpEvent<User>>{
    return this.http.post<User>(`${this.host}/user/updateProfileImage`, fromData,
    {reportProgress: true,
      observe: 'events'
    });
  }

  public deleteUser(username: string): Observable<CustomHttpRespone> {
    return this.http.delete<CustomHttpRespone>(`${this.host}/user/delete/${username}`);
  }

  public addUsersToLocalCache(users: User[]): void{
    localStorage.setItem('users', JSON.stringify(users));
  }

  public getUsersFromLocalCache(): User[] {
    if(localStorage.getItem('users')){
      return JSON.parse(localStorage.getItem('users'));
    }
    return null;
  }

  public createUserFormData(loggedInUsername: string, user: User, profileImage: File): FormData {
      const formData = new FormData();
      formData.append('currentUsername', loggedInUsername);
      formData.append('firstName', user.firstName);
      formData.append('lastName', user.lastName);
      formData.append('username', user.username);
      formData.append('email', user.email);
      formData.append('role', user.role);
      formData.append('profileImage', profileImage);
      formData.append('isActive', JSON.stringify(user.active));
      formData.append('isNotLocked', JSON.stringify(user.notLocked));
      return formData;
  }
}
