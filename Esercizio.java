package benessereApp;

import java.io.*;
import java.util.*;

public class SaluteApp {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        int scelta = -1, reinserisci = 0;
        double fabbisogno = 0, calorieTotali = 0;
        boolean fabbisognoImpostato = false, datiInseriti = false;
        String sesso = "", record, alimento;
        int altezza = 0, eta = 0;

        File archivioDati = new File("data/datiUtente.csv");

        System.out.println("=== Programma Benessere Personale ===");

        while (scelta != 5) {
            System.out.println("\n1) Calcolo peso ideale");
            System.out.println("2) Calcolo fabbisogno calorico");
            System.out.println("3) Monitoraggio alimentare");
            System.out.println("4) Svuota registro giornaliero");
            System.out.println("5) Esci");
            System.out.print("Scelta: ");

            try {
                scelta = Integer.parseInt(input.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Inserire un numero valido.");
                continue;
            }

            switch (scelta) {
                case 1:
                    if (archivioDati.exists()) {
                        System.out.print("Dati trovati. Vuoi reinserirli? (1=si, 0=no): ");
                        try {
                            reinserisci = Integer.parseInt(input.nextLine());
                        } catch (Exception e) {
                            reinserisci = 1;
                        }
                    }

                    if (!archivioDati.exists() || reinserisci == 1) {
                        do {
                            System.out.print("Sesso (M/F): ");
                            sesso = input.nextLine().trim().toUpperCase();
                        } while (!sesso.equals("M") && !sesso.equals("F"));

                        do {
                            System.out.print("Altezza in cm: ");
                            altezza = Integer.parseInt(input.nextLine());
                        } while (altezza <= 0 || altezza > 300);

                        do {
                            System.out.print("Età: ");
                            eta = Integer.parseInt(input.nextLine());
                        } while (eta <= 0 || eta > 150);
                    } else {
                        try (Scanner lettore = new Scanner(archivioDati)) {
                            lettore.nextLine();
                            record = lettore.nextLine();
                            String[] valori = record.split("\\|");
                            sesso = valori[0].trim();
                            altezza = Integer.parseInt(valori[1].trim());
                            eta = Integer.parseInt(valori[2].trim());
                        }
                    }

                    CalcolatorePeso pesoIdeale = new CalcolatorePeso(altezza, eta, sesso);
                    pesoIdeale.stampaPesi();

                    System.out.print("Scegli un indice (1-9) per salvare il peso ideale: ");
                    int indicePeso = Integer.parseInt(input.nextLine());

                    try (FileWriter writer = new FileWriter("data/datiUtente.csv")) {
                        writer.write("Sesso | Altezza | Eta | Peso\n");
                        writer.write(sesso + " | " + altezza + " | " + eta + " | " + pesoIdeale.getPesoByIndice(indicePeso) + "\n");
                    }

                    datiInseriti = true;
                    break;

                case 2:
                    if (!datiInseriti) {
                        do {
                            System.out.print("Sesso (M/F): ");
                            sesso = input.nextLine().trim().toUpperCase();
                        } while (!sesso.equals("M") && !sesso.equals("F"));

                        do {
                            System.out.print("Età: ");
                            eta = Integer.parseInt(input.nextLine());
                        } while (eta <= 0 || eta > 150);
                    }

                    System.out.print("Peso attuale (kg): ");
                    double peso = Double.parseDouble(input.nextLine());

                    System.out.print("Giorni di attività aerobica alla settimana: ");
                    int giorni = Integer.parseInt(input.nextLine());

                    class Energia {
                        private int eta;
                        private double peso;
                        private String sesso;

                        public Energia(int eta, double peso, String sesso) {
                            this.eta = eta;
                            this.peso = peso;
                            this.sesso = sesso;
                        }

                        public double calcolaBasale() {
                            if (eta < 29)
                                return sesso.equals("F") ? 14.7 * peso + 496 : 15.3 * peso + 679;
                            else if (eta < 60)
                                return sesso.equals("F") ? 8.7 * peso + 829 : 11.6 * peso + 879;
                            else if (eta < 75)
                                return sesso.equals("F") ? 9.2 * peso + 688 : 11.9 * peso + 700;
                            else
                                return sesso.equals("F") ? 9.8 * peso + 624 : 8.4 * peso + 819;
                        }

                        public int getEta() {
                            return eta;
                        }

                        public String getSesso() {
                            return sesso;
                        }
                    }
                    
                    class Attivita {
                        private Energia persona;
                        private int giorni;

                        public Attivita(Energia persona, int giorni) {
                            this.persona = persona;
                            this.giorni = giorni;
                        }

                        public double calcolaFattore() {
                            String sesso = persona.getSesso();
                            int eta = persona.getEta();

                            if (sesso.equals("M"))
                                return (eta < 59) ? (giorni < 3 ? 1.55 : giorni < 5 ? 1.78 : 2.10) : 1.51;
                            else
                                return (eta < 59) ? (giorni < 3 ? 1.56 : giorni < 5 ? 1.64 : 1.82) : 1.56;
                        }
                    }

                    Energia metabolismo = new Energia(eta, peso, sesso);
                    double mb = metabolismo.calcolaBasale();

                    Attivita att = new Attivita(metabolismo, giorni);
                    double fattore = att.calcolaFattore();

                    fabbisogno = mb * fattore;
                    fabbisognoImpostato = true;

                    System.out.printf("Fabbisogno giornaliero: %.2f kcal%n", fabbisogno);
                    break;

                case 3:
                    if (!fabbisognoImpostato) {
                        System.out.println("Calcola prima il fabbisogno calorico (opzione 2).");
                        break;
                    }

                    File giornaliero = new File("data/registroCalorie.csv");
                    boolean nuovoFile = !giornaliero.exists() || giornaliero.length() == 0;

                    try (FileWriter out = new FileWriter(giornaliero, true)) {
                        if (nuovoFile) out.write("Alimento | Calorie\n");

                        double rimanente = fabbisogno;
                        boolean continua = true;

                        while (continua) {
                            System.out.print("Nome alimento: ");
                            alimento = input.nextLine().trim();

                            System.out.print("Quantità in grammi: ");
                            int grammi = Integer.parseInt(input.nextLine());

                            File tabella = new File("data/listaCibi.csv");
                            Scanner scannerTabella = new Scanner(tabella);
                            scannerTabella.nextLine();

                            boolean trovato = false;
                            int kcal = 0;

                            while (scannerTabella.hasNextLine()) {
                                String linea = scannerTabella.nextLine();
                                String[] campi = linea.split("\\|");
                                String nomeTabella = campi[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                                String nomeInput = alimento.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();

                                if (nomeTabella.contains(nomeInput)) {
                                    int pesoCampione = Integer.parseInt(campi[1].replaceAll("[^0-9]", ""));
                                    kcal = Integer.parseInt(campi[2].trim());
                                    int calAssunte = grammi * kcal / pesoCampione;
                                    rimanente -= calAssunte;
                                    calorieTotali += calAssunte;
                                    out.write(alimento + " | " + calAssunte + "\n");
                                    trovato = true;

                                    System.out.println("Calorie assunte: " + calAssunte);
                                    break;
                                }
                            }
                            scannerTabella.close();

                            if (!trovato) {
                                System.out.println("Cibo non trovato.");
                            }

                            if (rimanente <= 0) {
                                System.out.println("Hai raggiunto o superato il fabbisogno.");
                                continua = false;
                            } else {
                                System.out.println("Rimanenti: " + rimanente);
                                System.out.print("Aggiungere altro cibo? (1=Sì, 0=No): ");
                                int risposta = Integer.parseInt(input.nextLine());
                                if (risposta == 0) continua = false;
                            }
                        }

                    } catch (IOException e) {
                        System.out.println("Errore nella scrittura del file: " + e.getMessage());
                    }
                    break;

                case 4:
                    try (FileWriter reset = new FileWriter("data/registroCalorie.csv", false)) {
                        reset.write("");
                        System.out.println("Registro giornaliero svuotato.");
                    } catch (IOException e) {
                        System.out.println("Errore nel reset: " + e.getMessage());
                    }
                    break;

                case 5:
                    System.out.println("Uscita in corso...");
                    break;

                default:
                    System.out.println("Scelta non valida. Seleziona tra 1 e 5.");
            }
        }

        input.close();
    }
}
}