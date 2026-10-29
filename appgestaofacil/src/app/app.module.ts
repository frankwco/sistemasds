import { LOCALE_ID, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { RouteReuseStrategy } from '@angular/router';

import { IonicModule, IonicRouteStrategy } from '@ionic/angular';
import { SplashScreen } from '@ionic-native/splash-screen/ngx';
import { StatusBar } from '@ionic-native/status-bar/ngx';
import { Camera } from '@ionic-native/camera/ngx';
import { File } from '@ionic-native/file/ngx';
import { SpeechRecognition } from '@ionic-native/speech-recognition/ngx';
import { Media } from '@ionic-native/media/ngx';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HttpModule } from '@angular/http';
import { HTTP_INTERCEPTORS, HttpClientModule } from  '@angular/common/http';
import { ChaveApiInterceptor } from './api/chave-api.interceptor';
import {LoginService} from './api/login.service';
import {PontoColetaService} from './service/ponto-coleta.service';
import {AtualizarDadosService} from './api/atualizar-dados.service';
import {DatabaseService} from './service/database.service';
import { IonicStorageModule } from '@ionic/storage';

import { SQLitePorter } from '@ionic-native/sqlite-porter/ngx';
import { SQLite } from '@ionic-native/sqlite/ngx';
import { SignaturePadModule } from 'angular2-signaturepad';

import { Clipboard } from '@ionic-native/clipboard/ngx';
import { environment } from '../environments/environment';
import { AngularFireModule } from '@angular/fire';
import { AngularFirestoreModule } from '@angular/fire/firestore';


import { AngularFireStorageModule } from '@angular/fire/storage';

import { AngularFireAuthModule } from '@angular/fire/auth';

import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
registerLocaleData(localePt);


@NgModule({
  declarations: [AppComponent],
  entryComponents: [],
  imports: [HttpClientModule, 
    HttpModule, 
    CommonModule, 
    BrowserModule, 
    IonicModule.forRoot(), 
    IonicStorageModule.forRoot(), 
    AppRoutingModule,
    SignaturePadModule,
    AngularFireModule.initializeApp(environment.firebaseConfig,'appgestaofacil'),
    AngularFirestoreModule,
    AngularFireStorageModule,
    AngularFireAuthModule,
    AngularFirestoreModule.enablePersistence()
   ],
  providers: [
    Clipboard,
    StatusBar,
    SplashScreen,
    { provide: RouteReuseStrategy, useClass: IonicRouteStrategy },
    { provide: HTTP_INTERCEPTORS, useClass: ChaveApiInterceptor, multi: true },
    LoginService,
    AtualizarDadosService,
    PontoColetaService,
    DatabaseService,
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    SQLite,
    SQLitePorter,
    Camera,
    File,
    SpeechRecognition,
    Media
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
