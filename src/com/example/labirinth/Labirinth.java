package com.example.labirinth;

import java.io.*;
import java.util.ArrayDeque;
import java.util.Scanner;

public class Labirinth
{
    private static int n; // stevilo vrstic oz. stolpcev
    private static Node [][] mreza; // mreza nodeov
    private static Node start; // kaže na start, da ga ne rabim pol neki iskat
    private static boolean cilj_najden = false; // ali je najden cilj
    private static ArrayDeque nodes; // za BFS
    private static int cost; // kolikšen je celoten cost
    private static int st_obiskanih_nodeov; // koliko nodeov smo obiskali
    private static int [][] tabela;
    private static String path;
    private static int stevilo_ciljev = 0;
    private static Node [] cilji;
    private static Node cilj;
    private static int odmik;
    private static Node [] tabela_za_astar;
    private static int stevec_za_astar = 0;
    public static class Node
    {
        int x, y; // njegova x in y pozicija
        int value; // njegov value oz. cost
        Node gor, dol, levo, desno; // njegovi sosedje
        boolean visited; // ali smo ga že obiskali?
        Node previous; // kako smo prišli do njega
        Node [] sosedi; // kateri so njegovi sosedje
        float distance;
        boolean final_path;
        int st_sosedov = 0;
        Node(int x, int y, int value)
        {
            this.final_path = false;
            this.x = x;
            this.y = y;
            this.value = value;
            this.gor = null;
            this.dol = null;
            this.levo = null;
            this.desno = null;
            this.visited = false;
            this.previous = null;
            this.distance = 0;
        }
        void prestejSosede()
        {
            // preštejemo koliko sosedov ima
            if(dol != null) // �?e obstaja spodnji sosed
                st_sosedov++;
            if(levo != null) // �?e obstaja levi sosed
                st_sosedov++;
            if(desno != null) // �?e obstaja desni sosed
                st_sosedov++;
            if(gor != null) // �?e obstaja gornji sosed
                st_sosedov++;
        }
        void nafilajSosede()
        {
            prestejSosede();
            // ----------------------------------------------------------- DOL, LEVO, DESNO, GOR
            sosedi = new Node[st_sosedov];
            st_sosedov = 0;
            if(dol != null) // �?e obstaja spodnji sosed
                sosedi[st_sosedov++] = dol; // ga dodamo v seznam
            if(levo != null) // �?e obstaja levi sosed
                sosedi[st_sosedov++] = levo; // ga dodamo v seznam
            if(desno != null) // �?e obstaja desni sosed
                sosedi[st_sosedov++] = desno; // ga dodamo v seznam
            if(gor != null) // �?e obstaja gornji sosed
                sosedi[st_sosedov++] = gor; // ga dodamo v seznam
        }
        void calculateDistance()
        {
            this.distance = razdalja(this.x, cilj.x, this.y, cilj.y);
        }
    }
    private static int [][] readFile(String filepath) throws IOException {
        File file = new File(filepath);
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            n = line.split(",").length-2; // prvega in zadnjega stolpca ne štejeva, ker želiva imeti samo 'uporaben' labirint
            mreza = new Node[n][n]; // ustvariva mrezo z n*n elementi
            int[][] tabela = new int[n][n]; // in tud tabelo, v kateri bojo costi za vsako polje
            int j = 0;
            while (j++ != n) // da zadnjo vrstico, ki je polna "-1" spusti
            {
                String[] delcki = br.readLine().split(","); // posplita po ","
                for (int i = 0; i < n; i++) // gre povrsti
                    tabela[j-1][i] = Integer.valueOf(delcki[i + 1]); // za�?enši z prvim del�?kom, ker je 0-ti "-1"
            }
            return tabela;
        }
        catch (FileNotFoundException e) {e.printStackTrace();}
        return null;
    }
    private static void printMreza()
    {
        for(int i = 0; i < n; i++)
        {
            for(int j = 0; j < n; j++)
                System.out.print(mreza[i][j].value + "\t");
            System.out.println();
        }
    }
    private static void drawMreza()
    {
        for(int i = n-1; i >= 0; i--)
            for(int j = 0; j < n; j++)
                drawPolje(mreza[i][j]);
    }
    private static void drawPolje(Node node)
    {
        int x = node.x * odmik + odmik;
        int y = (n-node.y-1) * odmik + odmik;

        if(node.value == -1) // ZID
            StdDraw.setPenColor(0,0,0); // ČRNA
        else if(node.value == -2) // START
            StdDraw.setPenColor(255,100,100); //
        else if(node.value == -3) // CILJ
            StdDraw.setPenColor(0,100,0);
        else if(node.final_path) // FINAL PATH
            StdDraw.setPenColor(200, 255, 150);
        else if(node.visited) // JUST VISITED
            StdDraw.setPenColor(0,128,255);
        else
            StdDraw.setPenColor(50,55,250);

        StdDraw.filledSquare(x, y, odmik/2);
        StdDraw.setPenColor(0,0,0); // ČRNA
        StdDraw.setPenRadius(0.01);
        StdDraw.square(x,y,odmik/2);


        if(node.value == -2)
            StdDraw.text(x, y, "S");
        else if(node.value == -3)
            StdDraw.text(x, y, "F");
        else
            StdDraw.text(x, y, String.valueOf(node.value));

    }
    private static void nafilajMrezo()
    {
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++)
                mreza[i][j] = new Node(j, i, tabela[i][j]); // nafilava mrežo z node-i

        Node trenutn_node;
        // dodava vse povezave
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++)
            {

                trenutn_node = mreza[i][j]; // se premakneva na trenutni node
                if(trenutn_node.value == -2) // pogledava, �?e je start
                    start = trenutn_node; // �?e je, narediva neko bližnjico
                if(trenutn_node.value == -3)
                    stevilo_ciljev++;
                // Nafilava robne node
                if(i == 0) // �?e je v najvišji vrstici
                    trenutn_node.gor = null; // pomeni, da gor ne more
                if(j == 0) // �?e je v levem stolpcu
                    trenutn_node.levo = null; // pomeni, da levo ne more
                if(i == n-1) // �?e je v spodnji vrstici
                    trenutn_node.dol = null; // pomeni, da dol ne more
                if(j == n-1) // �?e je v desnem stolpcu
                    trenutn_node.desno = null; // pomeni, da desno ne more

                // in še vse ostale
                // pregledava �?e ni robn in �?e ni zid
                // �?e je to kul, nastaviva povezavo, druga�?e ne
                if(j < n-1 && mreza[i][j+1].value != -1)
                    trenutn_node.desno = mreza[i][j+1];
                if(i < n-1 && mreza[i+1][j].value != -1)
                    trenutn_node.dol = mreza[i+1][j];
                if(i > 0 && mreza[i-1][j].value != -1)
                    trenutn_node.gor = mreza[i-1][j];
                if(j > 0 && mreza[i][j-1].value != -1)
                    trenutn_node.levo = mreza[i][j-1];
            }
        cilji = new Node [stevilo_ciljev];
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++) {
                mreza[i][j].nafilajSosede(); // vsakmu node-u nafilava mrežo s sosedi
            }
    }
    private static float razdalja(int x1, int x2, int y1, int y2)
    {
        return (float) Math.sqrt((float) Math.pow(x1-x2,2)+(float) Math.pow(y1-y2,2));
    }
    private static String DFS(Node kje_sm)
    {
        String vrni = "";
        if(kje_sm.value == -3) // ko najdeva cilj
        {
            cilj_najden = true; // cilj == najdem
            System.out.println("Stevilo vseh obiskanih node-ov: " + st_obiskanih_nodeov );
            return "\\\\"+ kje_sm.y + " " + kje_sm.x + "//"; // da imava v pathu tudi kon�?en node
        }
        kje_sm.visited = true; // da je ta node že bil obiskan
        st_obiskanih_nodeov++; // preštejeva koliko nodov sva obiskala
        for(Node nod : kje_sm.sosedi) // gre �?ez vse njegove sosede
        {
            if(!nod.visited) // �?e ta nod še ni bil obiskan
            {
                vrni += "\\\\"+ kje_sm.y + " " + kje_sm.x + "//" + DFS(nod);
                if(!cilj_najden) // �?e dava to stran, dobiva celotno pot po kateri se sprehaja
                    vrni = ""; // resetirava path
            }
            if(cilj_najden) // ko najdeva cilj
                break;
        }
        return vrni;
    }
    private static String BFS(Node kje_sm)
    {
        if(kje_sm.value == -3) // �?e je to cilj
        {
            cilj_najden = true; // našli smo cilj
            String pathh = ""; // napiše pot, ki smo jo najdl
            while(kje_sm.value != -2) // dokler ne prideš nazaj do za�?etka
            {
                pathh = "\\\\" + kje_sm.y + " " + kje_sm.x + "//" + pathh; // dopolnjuj pot
                kje_sm = kje_sm.previous; // pojdi en korak nazaj
            }
            System.out.println("Stevilo obiskanih node-ov: " + st_obiskanih_nodeov );
            return "\\\\" + kje_sm.y + " " + kje_sm.x + "//" + pathh; // vrni pot po kateri si prišel vklju�?no s sabo
        }
        Node trenutn_node = kje_sm; // da se lahko sprehajam
        if(trenutn_node.visited) // ker se je zgodil, da je že bil v QUE preden je bil visited
            return BFS((Node) nodes.removeFirst()); // zato, ko pridem do sm, samo kli�?em naprej

        trenutn_node.visited = true; // da povem, da sem tukej že bil
        st_obiskanih_nodeov++; // preštejeva koliko nodov sva obiskala

        for(Node nod : trenutn_node.sosedi) // dodam vse njegove sosede
        {
            if(!nod.visited) // �?e še niso bili obiskani
            {
                nodes.addLast(nod); // dodaj vse node na katere mejiš (si njihov sosed)
                nod.previous = trenutn_node; // povem na kak na�?in sem prišel do tega noda, ki ga dodajam
            }
            if(cilj_najden)
                break;
        }
        // rekurzivno kli�?i dokler ne najdeš cilja
        // argument je pa element v vrsti, ki je bil prvi dodan
        return BFS((Node) nodes.removeFirst());

    }
    private static String Dijkstra(Node kje_sm)
    {
        Node [] all_nodes = new Node[n*n]; // tukej notr bojo vsi node-i v 1D arrayu
        int k = 0, kk = 0;

        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++)
            {
                if (mreza[i][j].value != -2) // �?e node ni enak za�?etku
                    mreza[i][j].distance = Integer.MAX_VALUE; // Distance do cilja je inf

                if(mreza[i][j].value == -1) // ne želimo imeti zidov notr
                    continue;

                all_nodes[k++] = mreza[i][j]; // dodam v 1D array node-ov
                if(mreza[i][j].value == -3) // nafilam še vse cilje
                    cilji[kk++] = mreza[i][j];
            }
        int kok_je_vseh = k; // kolk je vseh node-ov
        st_obiskanih_nodeov = k;
        while(k > 0)
        {
            int min_index = 0;
            Node min = null;

            for (int i = 0; i < all_nodes.length; i++) // gre �?ez vse node-e
                if (all_nodes[i] != null) // in prvi, ki ni null
                {
                    min = all_nodes[i]; // ga proglasi za najmanjšega, da lahko za�?nem iskat minimum
                    min_index = i;
                    break;
                }

            // find min po kme�?ko
            for(int i = 0; i < kok_je_vseh; i++)
            {
                if(all_nodes[i] == null)
                    continue;

                if(all_nodes[i].distance < min.distance)
                {
                    min_index = i;
                    min = all_nodes[min_index];
                }
            }

            Node sosedje [] = min.sosedi; // vsi sosedi od minimuma
            Node sosed;
            for(int i = 0; i < sosedje.length; i++)
            {
                sosed = sosedje[i];
                if(sosed.visited) // �?e smo ga že obiskali, ga ne rabimo še enkrat
                    continue;

                if (min.distance + sosed.value < sosed.distance)
                {
                    sosed.previous = min; // da mamo path kako pridt do njega
                    sosed.distance = min.distance + sosed.value; // in na podlagi tega tud spremenimo njegov distance
                }
            }
            all_nodes[min_index].visited = true;
            all_nodes[min_index] = null;
            k--;
        }

        // Najdemo cilj, ki ima najmanjši distance => je najceneje pridt do njega
        int index_min_cost = 0;
        for(int i = 1; i < kk; i++)
            if(cilji[i].distance < cilji[index_min_cost].distance)
                index_min_cost = i;


        String pathh = ""; // napiše pot, ki smo jo najdl
        kje_sm = cilji[index_min_cost];
        while(kje_sm.value != -2) // dokler ne prideš nazaj do za�?etka
        {
            pathh = "\\\\" + kje_sm.y + " " + kje_sm.x + "//" + pathh; // dopolnjuj pot
            kje_sm = kje_sm.previous; // pojdi en korak nazaj
        }
        System.out.println("Stevilo obiskanih node-ov: " + st_obiskanih_nodeov );
        return "\\\\" + kje_sm.y + " " + kje_sm.x + "//" + pathh;

    }
    private static void setFinishes()
    {
        int kk = 0;
        for(int i = 0; i < n; i++)
            for(int j = 0; j < n; j++)
                if(mreza[i][j].value == -3) // nafilam še vse cilje
                    cilji[kk++] = mreza[i][j];
    }
    private static boolean contains(Node [] tabela, Node x)
    {
        for(int i = 0; i < tabela.length; i++)
            if(tabela[i] == x)
                return true;
        return false;
    }
    private static Node findMin(Node [] tabela)
    {
        Node mini = null;
        float minimum = Float.MAX_VALUE;
        for (Node x : tabela)
            if (x != null && !x.visited && x.distance < minimum)
            {
                minimum = x.distance;
                mini = x;
            }
        return mini;
    }
    private static String A_Star(Node kje_sm)
    {
        if(kje_sm == cilj) // �?e je to cilj, ki ga iš�?emo
        {
            cilj_najden = true; // našli smo cilj
            String pathh = ""; // napiše pot, ki smo jo najdl
            while(kje_sm.value != -2) // dokler ne prideš nazaj do za�?etka
            {
                pathh = "\\\\" + kje_sm.y + " " + kje_sm.x + "//" + pathh; // dopolnjuj pot
                kje_sm = kje_sm.previous; // pojdi en korak nazaj
            }
            return "\\\\" + kje_sm.y + " " + kje_sm.x + "//" + pathh + "W" + st_obiskanih_nodeov; // vrni pot po kateri si prišel vklju�?no s sabo + koliko node-ov si obiskal
        }
        Node trenutn_node = kje_sm; // da se lahko sprehajam

        trenutn_node.visited = true; // da povem, da sem tukej že bil
        st_obiskanih_nodeov++; // preštejeva koliko nodov sva obiskala

        for(Node x : kje_sm.sosedi) // dodava vse sosede
            if(!x.visited || !contains(tabela_za_astar, x)) // ki še niso bili obiskani oz. še niso v tabeli
            {
                x.calculateDistance();
                x.previous = kje_sm;
                tabela_za_astar[stevec_za_astar++] = x; // jih dodava
            }
        // rekurzivno kli�?i dokler ne najdeš cilja
        Node min = findMin(tabela_za_astar);
        return A_Star(min);
    }
    private static int fromPathgetCost(String path, boolean izpis)
    {
        cost = 0;
        if(izpis)
            System.out.print("|"); // da se izpis za�?ne z "|"
        path = path.replace("\\", ""); // najprej umaknem "\\"
        String [] delcki = path.split("//"); // nato splittam po "//"
        for(String xpay : delcki) //da vn dobim obe koordinati
        {
            String [] neki = xpay.split(" "); // še to splittam po " "
            cost += mreza[Integer.valueOf(neki[0])][Integer.valueOf(neki[1])].value; // in nato odštejem to od skupnega costa
            mreza[Integer.valueOf(neki[0])][Integer.valueOf(neki[1])].final_path = true;
            if(izpis)
                System.out.print(neki[1] + " " + neki[0] + "|"); // da imava najprej x in nato y
        }
        if(izpis)
            System.out.println("\nCena te poti je : "+ (cost+5)); // zrad tega, ker upošteva tudi za�?eten kon�?en node
        return cost+5;
    }
    private static void init() throws IOException
    {
        StdDraw.setCanvasSize(800,800);
        StdDraw.setXscale(0, 800);
        StdDraw.setYscale(0, 800);
        nafilajMrezo();
        //printMreza();
        cost = 0;
        st_obiskanih_nodeov = 0;
        nodes = new ArrayDeque();
        odmik = 800 / (n+1);

    }
    private static void DFS() throws IOException
    {
        init();
        System.out.println("\n<<---DFS--->>");
        path = DFS(start);
        fromPathgetCost(path, true);
        System.out.println("<<---DFS--->>\n");
    }
    private static void BFS() throws  IOException
    {
        init();
        System.out.println("<<---BFS--->>");
        path = BFS(start);
        fromPathgetCost(path, true);
        System.out.println("<<---BFS--->>\n");
    }
    private static void Dijkstra() throws IOException
    {
        init();
        System.out.println("<<---Dijkstra--->>");
        path = Dijkstra(start);
        fromPathgetCost(path, true);
        System.out.println("<<---Dijkstra--->>\n");
    }
    private static void A_Star() throws IOException
    {
        System.out.println("<<---A Star--->>");
        int min_st_obiskanih = Integer.MAX_VALUE;
        int the_best = 0; // index za najboljšo rešitev (aka najbližji cilj)
        int min_cost = Integer.MAX_VALUE;
        for(int i = 0; i < stevilo_ciljev; i++)
        {
            init(); // da vsaki�? znova inicializira
            setFinishes(); // in nastavi cilje
            cilj = cilji[i]; //
            path = A_Star(start); // požene algoritem
            int st_obiskanih = Integer.valueOf(path.split("W")[1]); // potegnem vn st_obiskanih
            int cost = fromPathgetCost(path.split("W")[0], false); //  in izra�?unam cost te poti
            if (cost < min_cost || (cost == min_cost && min_st_obiskanih > st_obiskanih)) // najdem najcenejšo pot
            {
                the_best = i;
                min_cost = cost;
                min_st_obiskanih = st_obiskanih;
            }
        }
        // in potem še enkrat poženem ta algoritem, da lepo prikaže pot
        init();
        setFinishes();
        cilj = cilji[the_best];
        path = A_Star(start);
        int st_obiskanih = Integer.valueOf(path.split("W")[1]);
        System.out.println("Stevilo obiskanih node-ov : " + st_obiskanih);
        fromPathgetCost(path.split("W")[0], true);

        System.out.println("<<---A Star--->>\n");
    }
    public static void main(String[] args) throws IOException {

        System.out.println("Seminarska naloga pri UI");
        tabela = readFile("/Users/Asus/Documents/Faks/UI/Seminarski nalogi/Labirint/labyrinth_3.txt");
        String kater_algoritem;
        tabela_za_astar = new Node[n*n];
        boolean debug = true;
        if(!debug)
        {
            Scanner sc = new Scanner(System.in);
            System.out.print("Kater algoritem želiš pognati? : ");
            kater_algoritem = sc.nextLine();
        }
        else
            kater_algoritem = "AStar";

        switch (kater_algoritem)
        {
            case "DFS": DFS(); break;
            case "BFS": BFS(); break;
            case "Dijkstra": Dijkstra(); break;
            case "AStar": A_Star(); break;
        }
        drawMreza();
    }
}
