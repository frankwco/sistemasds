import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-cliente',
  templateUrl: './cliente.page.html',
  styleUrls: ['./cliente.page.scss'],
})
export class ClientePage implements OnInit {

    isItemAvailable = true;
    items = [];
    constructor() {
    this.items = ["José Antonio","Farmácia Brasil", "Laboratório Bom Jardim"];
    }
   

  ngOnInit() {
  }

}
