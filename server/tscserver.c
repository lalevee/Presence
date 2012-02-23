#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <errno.h>

void afficherAdresse(struct sockaddr_in adr);

#define    DEFPORT  5234
#define	   BSIZE    80

int main(int argc, char *argv[]) {
  int ecoute, canal, sent = 0, received = 0, ret;
  char buf[BSIZE];
  struct sockaddr_in adrEcoute, reception;
  unsigned int receptionlen;
  short port = DEFPORT;
  
  if (argc == 2) {
    port = (short) atoi(argv[1]);
    if (port < 1024) {
      fprintf(stderr, "Error- This port is reserved for root user\n");
      exit(1);
    }
  }

  printf("tscserver: listening on port %d\n", port);

  ecoute = socket (AF_INET, SOCK_STREAM, 0);
  if (ecoute < 0) {
    perror("socket");
    exit(1);
  }
  
  adrEcoute.sin_family = AF_INET;
  adrEcoute.sin_addr.s_addr = INADDR_ANY;
  adrEcoute.sin_port = htons(port);

  ret = bind (ecoute,  (struct sockaddr *) &adrEcoute, sizeof(adrEcoute));
  if (ret < 0) {
    perror ("bind");
    exit(1);
  }
  
  ret = listen (ecoute, 5);
  if (ret < 0) {
    perror ("listen");
    exit(1);
  }
  
  receptionlen = sizeof(reception);
  canal = accept(ecoute, (struct sockaddr *) &reception, &receptionlen);
  if (canal < 0) {
    perror("accept");
    exit(1);
  }
  
  do {
    received = read(canal, buf, BSIZE);
    if (received < 0) {
      perror("recv");
      exit(1);
    }
    if (received == 0) {
      printf("ZERO\n");
    }
    else {
      buf[received] = 0;
      printf("Connection request: \"%s\"\n", buf);
    }
    strcpy(buf, "OK\n");
    sent = write(canal, buf, 3);
    if (sent < 0) {
      perror("send");
      exit(1);
    }
  } while (buf[0] != 'Q');


  afficherAdresse(reception);
    
  close(canal);
  close(ecoute);
  return 0;
}

/* impression d'adresse pour debug */

void afficherAdresse(struct sockaddr_in adr) {
  union {
    unsigned char octet[4];
    unsigned int adr;
  } addrOctet;
  int i;
  printf("adresse IP = %x (", adr.sin_addr.s_addr);
  addrOctet.adr = adr.sin_addr.s_addr;
  for (i=0; i<4; i++) {
    printf("%d", addrOctet.octet[i]);
    if (i<3) printf(".");
  }
  printf("), port = %d\n", ntohs(adr.sin_port));
}
