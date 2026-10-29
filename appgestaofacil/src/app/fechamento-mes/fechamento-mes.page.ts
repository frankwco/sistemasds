import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-fechamento-mes',
  templateUrl: './fechamento-mes.page.html',
  styleUrls: ['./fechamento-mes.page.scss'],
})
export class FechamentoMesPage implements OnInit {
  isItemAvailable = true;
  items = [];
  constructor() {
  this.items = ["José Antonio","Farmácia Brasil", "Laboratório Bom Jardim"];
  }

  ngOnInit() {
  }

}
