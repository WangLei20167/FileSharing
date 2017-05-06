//
// Created by Administrator on 2017/02/03.
//

#ifndef FILESHARING_GF_H
#define FILESHARING_GF_H

#ifdef _cplusplus
extern "C" {
#endif

typedef unsigned int GFType;
extern GFType* table_alpha;
extern GFType* table_index;
extern GFType** table_mul;
extern GFType** table_div;

void gf_init(unsigned int m, unsigned int prim);
void gf_uninit();
void gf_print();

#define  gf_alpha(n)   (table_alpha[n])
#define  gf_index(n)   (table_index[n])

#define  gf_add(a,b)	(a^b)
#define  gf_sub(a,b)	(a^b)

#define  gf_mul(a,b)	(table_mul[a][b])
#define  gf_div(a,b)	(table_div[a][b])

#define  gf_neg(a)		(a)
#define  gf_inv(a)		(table_div[1][a])
GFType gf_exp(GFType a, GFType n);

#ifdef _cplusplus
}
#endif

#endif //FILESHARING_GF_H
