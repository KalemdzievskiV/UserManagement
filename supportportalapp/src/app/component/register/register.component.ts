import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Observer, Subscription } from 'rxjs';
import { NotificationType } from 'app/enum/notification-type.emun';
import { User } from 'app/model/user';
import { AuthenticationService } from 'app/service/authentication.service';
import { NotificationService } from 'app/service/notification.service';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit, OnDestroy {
  public showLoading: boolean;
  private subscription: Subscription[] = [];

  constructor(private router: Router, private authenticationService: AuthenticationService,
              private notificationService: NotificationService) { }

  ngOnInit(): void {
    if(this.authenticationService.isUserLoggedIn()){
      this.router.navigateByUrl('/user/managemant');
    }
  }

  public onRegister(user: User): void{
    this.showLoading = true;
    this.subscription.push(
      this.authenticationService.register(user).subscribe(
        (response : User) => {
          this.showLoading = false;
          this.sendNotification(NotificationType.SUCCESS, `A new account was created for ${response.firstName}.
          Please check your email for password`);

        },
        (error: HttpErrorResponse) => {
          console.log(error);
          this.sendNotification(NotificationType.ERROR, error.error.message);
          this.showLoading = false;
        }
      )
    )
  }
  private sendNotification(notificationType: NotificationType, message: string): void {
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
