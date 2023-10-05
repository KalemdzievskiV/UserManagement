import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observer, Subscription } from 'rxjs';
import { HeaderType } from 'app/enum/header-type.enum';
import { NotificationType } from 'app/enum/notification-type.emun';
import { User } from 'app/model/user';
import { AuthenticationService } from 'app/service/authentication.service';
import { NotificationService } from 'app/service/notification.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit, OnDestroy {

  public showLoading: boolean;
  private subscription: Subscription[] = [];

  constructor(private router: Router, private authenticationService: AuthenticationService,
              private notificationService: NotificationService) { }

  ngOnInit(): void {
    if(this.authenticationService.isUserLoggedIn()){
      this.router.navigateByUrl('/user/managemant');
    }
    else{
      this.router.navigateByUrl('/login');
    }
  }

  public onLogin(user: User): void {
    this.showLoading = true;
    this.subscription.push(
      this.authenticationService.login(user).subscribe(
        (response: HttpResponse<User>) => {
          const token = response.headers.get(HeaderType.JWT_TOKEN);
          console.log(response.headers.get('Authorities'))
          this.authenticationService.saveToken(token);
          this.authenticationService.addUserToLocalCache(response.body);
          this.router.navigateByUrl('/user/managemant');
          this.showLoading = false;
        },
        (errorResponse: HttpErrorResponse) => {
          this.sendErrorNotification(NotificationType.ERROR, errorResponse.error.message);
          this.showLoading = false;
        }
      )
    );
  }
  private sendErrorNotification(notificationType: NotificationType, message: string): void {
    if(message){
      this.notificationService.notify(notificationType, message);
    }
    else{
      this.notificationService.notify(notificationType, 'AN ERROR OCCURED. PLEASE TRY AGAIN');
    }
  }

  ngOnDestroy(): void {
    this.subscription.forEach(sub => sub.unsubscribe());
  }

  

}
