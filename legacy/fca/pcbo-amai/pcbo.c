/*  
 *  PCbO: Parallel Close-by-One (AMAI Version; http://fcalgs.sourceforge.net/)
 *  
 *  Copyright (C) 2007-2009
 *  Petr Krajca, <petr.krajca@upol.cz>
 *  Jan Outrata, <jan.outrata@upol.cz>
 *  Vilem Vychodil, <vilem.vychodil@upol.cz>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 ******************************************************************************
 *
 *  Users in academia are kindly asked to cite the following resources if the
 *  program is used to pursue any research activities which may result in
 *  publications:
 *
 *  Krajca P., Outrata J., Vychodil V.: Parallel Recursive Algorithm for FCA.
 *  In: Proc. CLA 2008, CEUR WS, 433(2008), 71-82. ISBN 978-80-244-2111-7.
 *  http://sunsite.informatik.rwth-aachen.de/Publications/CEUR-WS/Vol-433/
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef WINNT
#include <windows.h>
#include <process.h>
#else
#include <pthread.h>
#endif

#define BIT		((unsigned long) 1)
#define NULL_LONG	((unsigned long) 0)
#define INT_SIZE	(sizeof (int))
#define LONG_SIZE	(sizeof (unsigned long))
#define ARCHBIT		((LONG_SIZE * 8) - 1)
#define BYTE_COUNT_A	(LONG_SIZE * int_count_a)
#define BYTE_COUNT_O	(LONG_SIZE * int_count_o)
#define BUFFER_BLOCK	1024
#define MAX_DECIMAL_INT_SIZE (8)
#define OUTPUT_BUF_CAPACITY (32 * MAX_DECIMAL_INT_SIZE)
struct str_int
{
  char str[MAX_DECIMAL_INT_SIZE];
  int length;
} *table_of_ints;
int attributes = 0;
int objects = 0;
int int_count_a = 0;
int int_count_o = 0;
int table_entries = 0;
int min_support = 0;
unsigned long *context;
unsigned long *(*cols)[ARCHBIT + 1];
int (*supps)[ARCHBIT + 1];
unsigned long upto_bit[ARCHBIT + 1];
int attr_offset = 0;
FILE *in_file;
FILE *out_file;
int cpus = 1;
int threads;
int para_level = 2;
int verbosity_level = 1;
#ifdef WINNT
typedef HANDLE thread_id_t;
#else
typedef pthread_t thread_id_t;
#endif
thread_id_t *thread_id;
int *thread_i;
unsigned char **thread_queue;
unsigned char **thread_queue_head;
unsigned char **thread_queue_limit;
unsigned long **thread_intents;
#ifdef WINNT
HANDLE output_lock;
#else
pthread_mutex_t output_lock = PTHREAD_MUTEX_INITIALIZER;
#endif
struct thread_stat
{
  int closures;
  int computed;
  int queue_length;
};
struct thread_stat *counts;
struct thread_stat initial_thr_stat;

int
get_next_integer (file, value)
     FILE *file;
     int *value;
{
  int ch = ' ';
  *value = -1;
  while ((ch != EOF) && ((ch < '0') || (ch > '9'))) {
    ch = fgetc (file);
    if (ch == '\n')
      return 1;
  }
  if (ch == EOF)
    return 0;
  *value = 0;
  while ((ch >= '0') && (ch <= '9')) {
    *value *= 10;
    *value += ch - '0';
    ch = fgetc (file);
  }
  ungetc (ch, file);
  *value -= attr_offset;
  if (*value < 0) {
    fprintf (stderr,
	     "Invalid input value: %i (minimum value is %i), quitting.\n",
	     *value + attr_offset, attr_offset);
    exit (1);
  }
  return 1;
}

#define PUSH_NEW_INTEGER(__value) \
{ \
  if (index >= buff_size) { \
      buff_size += BUFFER_BLOCK; \
      buff = (int *) realloc (buff, INT_SIZE * buff_size); \
      if (! buff) { \
  	  fprintf (stderr, "Cannot reallocate buffer, quitting.\n"); \
	  exit (4); \
      } \
  } \
  buff [index] = (__value); \
  index ++; \
}

void
table_of_ints_init (max)
     int max;
{
  int i;
  table_of_ints = malloc (sizeof (struct str_int) * max);
  for (i = 0; i < max; i++) {
    sprintf (table_of_ints[i].str, "%i", i);
    table_of_ints[i].length = strlen (table_of_ints[i].str);
  }
}

void
read_context (file)
     FILE *file;
{
  int last_value = -1, value = 0, last_attribute = -1, last_object = -1;
  int *buff, i, index = 0, row = 0;
  size_t buff_size = BUFFER_BLOCK;
  if (attr_offset < 0)
    attr_offset = 0;
  buff = (int *) malloc (INT_SIZE * buff_size);
  if (!buff) {
    fprintf (stderr, "Cannot allocate buffer, quitting.\n");
    exit (3);
  }
  while (get_next_integer (file, &value)) {
    if ((value < 0) && (last_value < 0))
      continue;
    if (value < 0) {
      last_object++;
      PUSH_NEW_INTEGER (-1);
    }
    else {
      if (value > last_attribute)
	last_attribute = value;
      PUSH_NEW_INTEGER (value);
      table_entries++;
    }
    last_value = value;
  }
  if (last_value >= 0) {
    last_object++;
    PUSH_NEW_INTEGER (-1);
  }
  objects = last_object + 1;
  attributes = last_attribute + 1;
  int_count_a = (attributes / (ARCHBIT + 1)) + 1;
  int_count_o = (objects / (ARCHBIT + 1)) + 1;
  context = (unsigned long *) malloc (LONG_SIZE * int_count_a * objects);
  if (!context) {
    fprintf (stderr, "Cannot allocate bitcontext, quitting.\n");
    exit (5);
  }
  memset (context, 0, LONG_SIZE * int_count_a * objects);
  for (i = 0; i < index; i++) {
    if (buff[i] < 0) {
      row++;
      continue;
    }
    context[row * int_count_a + (buff[i] / (ARCHBIT + 1))] |=
      (BIT << (ARCHBIT - (buff[i] % (ARCHBIT + 1))));
  }
  free (buff);
}

void
print_attributes (set)
     const unsigned long *set;
{
  int i, j, c, first = 1;
  char buf[OUTPUT_BUF_CAPACITY + MAX_DECIMAL_INT_SIZE + 2];
  int buf_ptr = 0;
  int locked = 0;
  if (verbosity_level <= 0)
    return;
  for (c = j = 0; j < int_count_a; j++) {
    for (i = ARCHBIT; i >= 0; i--) {
      if (set[j] & (BIT << i)) {
	if (!first)
	  buf[buf_ptr++] = ' ';
	strcpy (buf + buf_ptr, table_of_ints[c].str);
	buf_ptr += table_of_ints[c].length;
	if (buf_ptr >= OUTPUT_BUF_CAPACITY) {
	  buf[buf_ptr] = '\0';
	  buf_ptr = 0;
	  if (!locked) {
#ifdef WINNT
	    WaitForSingleObject (output_lock, INFINITE);
#else
	    pthread_mutex_lock (&output_lock);
#endif
	    locked = 1;
	  }
	  fputs (buf, out_file);
	}
	first = 0;
      }
      c++;
      if (c >= attributes)
	goto out;
    }
  }
out:
  buf[buf_ptr++] = '\n';
  buf[buf_ptr] = '\0';
  if (!locked) {
#ifdef WINNT
    WaitForSingleObject (output_lock, INFINITE);
#else
    pthread_mutex_lock (&output_lock);
#endif
  }
  fputs (buf, out_file);
#ifdef WINNT
  ReleaseMutex (output_lock);
#else
  pthread_mutex_unlock (&output_lock);
#endif
}

void
print_context_info (void)
{
  if (verbosity_level >= 2)
    fprintf (stderr, "(:objects %6i :attributes %4i :entries %8i)\n",
	     objects, attributes, table_entries);
}

void
print_thread_info (id, stat)
     int id;
     struct thread_stat stat;
{
  if (verbosity_level >= 2)
    fprintf (stderr,
	     "(:proc %3i :closures %7i :computed %7i :queue-length %3i)\n",
	     id, stat.closures, stat.computed, stat.queue_length);
}

void
print_initial_thread_info (levels, stat, last_level_concepts)
     int levels;
     struct thread_stat stat;
     int last_level_concepts;
{
  if (verbosity_level >= 2)
    fprintf (stderr,
	     "(:proc %3i :closures %7i :computed %7i :levels %i :last-level-concepts %i)\n",
	     0, stat.closures, stat.computed, levels + 1,
	     last_level_concepts);
}

void
initialize_algorithm (void)
{
  int i, j, x, y;
  unsigned long *ptr;
  unsigned long mask;
  unsigned long *cols_buff;
  for (i = 0; i <= ARCHBIT; i++) {
    upto_bit[i] = NULL_LONG;
    for (j = ARCHBIT; j > i; j--)
      upto_bit[i] |= (BIT << j);
  }
  cols_buff = (unsigned long *)
    malloc (LONG_SIZE * int_count_o * (ARCHBIT + 1) * int_count_a);
  memset (cols_buff, 0,
	  LONG_SIZE * int_count_o * (ARCHBIT + 1) * int_count_a);
  cols =
    (unsigned long *(*)[ARCHBIT + 1]) malloc (sizeof (unsigned long *) *
					      (ARCHBIT + 1) * int_count_a);
  supps =
    (int (*)[ARCHBIT + 1]) malloc (sizeof (int) * (ARCHBIT + 1) *
				   int_count_a);
  ptr = cols_buff;
  for (j = 0; j < int_count_a; j++)
    for (i = ARCHBIT; i >= 0; i--) {
      mask = (BIT << i);
      cols[j][i] = ptr;
      supps[j][i] = 0;
      for (x = 0, y = j; x < objects; x++, y += int_count_a)
	if (context[y] & mask) {
	  ptr[x / (ARCHBIT + 1)] |= BIT << (x % (ARCHBIT + 1));
	  supps[j][i]++;
	}
      ptr += int_count_o;
    }
  table_of_ints_init (attributes);
}

void
compute_closure (intent, extent, prev_extent, atr_extent, supp)
     unsigned long *intent, *extent, *prev_extent, *atr_extent;
     size_t *supp;
{
  int i, j, k, l;
  memset (intent, 0xFF, BYTE_COUNT_A);
  if (atr_extent) {
    *supp = 0;
    for (k = 0; k < int_count_o; k++) {
      extent[k] = prev_extent[k] & atr_extent[k];
      if (extent[k])
	for (l = 0; l <= ARCHBIT; l++)
	  if (extent[k] & (BIT << l)) {
	    for (i = 0, j =
		 int_count_a * (k * (ARCHBIT + 1) + l);
		 i < int_count_a; i++, j++)
	      intent[i] &= context[j];
	    (*supp)++;
	  }
    }
  }
  else {
    memset (extent, 0xFF, BYTE_COUNT_O);
    for (j = 0; j < objects; j++) {
      for (i = 0; i < int_count_a; i++)
	intent[i] &= context[int_count_a * j + i];
    }
  }
}

void
generate_from_node (intent, extent, start_int, start_bit, id)
     unsigned long *intent;
     unsigned long *extent;
     int start_int, start_bit;
     int id;
{
  int i, total;
  size_t supp = 0;
  unsigned long *new_extent;
  unsigned long *new_intent;
  new_intent = extent + int_count_o;
  new_extent = new_intent + int_count_a;
  total = start_int * (ARCHBIT + 1) + (ARCHBIT - start_bit);
  for (; start_int < int_count_a; start_int++) {
    for (; start_bit >= 0; start_bit--) {
      if (total >= attributes)
	return;
      total++;
      if ((intent[start_int] & (BIT << start_bit)) ||
	  (supps[start_int][start_bit] < min_support))
	continue;
      compute_closure (new_intent, new_extent, extent,
		       cols[start_int][start_bit], &supp);
      counts[id].closures++;
      if ((new_intent[start_int] ^ intent[start_int]) & upto_bit[start_bit])
	goto skip;
      for (i = 0; i < start_int; i++)
	if (new_intent[i] ^ intent[i])
	  goto skip;
      if (supp < min_support)
	goto skip;
      counts[id].computed++;
      print_attributes (new_intent);
      if (new_intent[int_count_a - 1] & BIT)
	goto skip;
      if (start_bit == 0)
	generate_from_node (new_intent, new_extent,
			    start_int + 1, ARCHBIT, id);
      else
	generate_from_node (new_intent, new_extent,
			    start_int, start_bit - 1, id);
    skip:
      ;
    }
    start_bit = ARCHBIT;
  }
  return;
}

#ifdef WINNT
unsigned __stdcall
#else
void *
#endif
thread_func (params)
     void *params;
{
  int id;
  id = *(int *) params;
  for (; thread_queue_head[id] < thread_queue[id];) {
    memcpy (thread_intents[id], thread_queue_head[id],
	    BYTE_COUNT_A + BYTE_COUNT_O);
    thread_queue_head[id] += BYTE_COUNT_A + BYTE_COUNT_O;
    generate_from_node (thread_intents[id],
			thread_intents[id] + int_count_a,
			*(int *) thread_queue_head[id],
			*(int *) (thread_queue_head[id] + INT_SIZE), id);
    thread_queue_head[id] += INT_SIZE << 1;
  }
#ifdef WINNT
  return 0;
#else
  return NULL;
#endif
}

void
parallel_generate_from_node (intent, extent, start_int, start_bit,
			     rec_level, id)
     unsigned long *intent;
     unsigned long *extent;
     int start_int, start_bit, rec_level;
     int id;
{
  int i, total;
  static int num = 0;
  size_t supp = 0;
  unsigned long *new_extent;
  unsigned long *new_intent;
  if (rec_level == para_level) {
    i = num % threads;
    memcpy ((unsigned long *) thread_queue[i], intent,
	    BYTE_COUNT_A + BYTE_COUNT_O);
    thread_queue[i] += BYTE_COUNT_A + BYTE_COUNT_O;
    *(int *) thread_queue[i] = start_int;
    thread_queue[i] += INT_SIZE;
    *(int *) thread_queue[i] = start_bit;
    thread_queue[i] += INT_SIZE;
    counts[i].queue_length++;
    if (thread_queue[i] == thread_queue_limit[i]) {
      total = thread_queue_limit[i] - thread_queue_head[i];
      thread_queue_head[i] =
	(unsigned char *) realloc (thread_queue_head[i], total << 1);
      thread_queue[i] = thread_queue_head[i] + total;
      thread_queue_limit[i] = thread_queue_head[i] + (total << 1);
    }
    num++;
    return;
  }
  new_intent = extent + int_count_o;
  new_extent = new_intent + int_count_a;
  total = start_int * (ARCHBIT + 1) + (ARCHBIT - start_bit);
  for (; start_int < int_count_a; start_int++) {
    for (; start_bit >= 0; start_bit--) {
      if (total >= attributes)
	goto out;
      total++;
      if ((intent[start_int] & (BIT << start_bit)) ||
	  (supps[start_int][start_bit] < min_support))
	continue;
      compute_closure (new_intent, new_extent, extent,
		       cols[start_int][start_bit], &supp);
      counts[id].closures++;
      if ((new_intent[start_int] ^ intent[start_int]) & upto_bit[start_bit])
	goto skip;
      for (i = 0; i < start_int; i++)
	if (new_intent[i] ^ intent[i])
	  goto skip;
      if (supp < min_support)
	goto skip;
      counts[id].computed++;
      print_attributes (new_intent);
      if (new_intent[int_count_a - 1] & BIT)
	goto skip;
      if (start_bit == 0)
	parallel_generate_from_node (new_intent, new_extent,
				     start_int + 1, ARCHBIT,
				     rec_level + 1, id);
      else
	parallel_generate_from_node (new_intent, new_extent,
				     start_int, start_bit - 1,
				     rec_level + 1, id);
    skip:
      ;
    }
    start_bit = ARCHBIT;
  }
out:
  if (rec_level == 0) {
    print_initial_thread_info (para_level, counts[0], num);
    initial_thr_stat = counts[0];
    counts[0].closures = 0;
    counts[0].computed = 0;
    for (i = 1; i < threads; i++)
      if (thread_queue_head[i] != thread_queue[i])
#ifdef WINNT
	thread_id[i] =
	  (HANDLE) _beginthreadex (NULL, 0, thread_func,
				   &thread_i[i], 0, NULL);
#else
	pthread_create (&thread_id[i], NULL, thread_func, &thread_i[i]);
#endif

    for (; thread_queue_head[id] < thread_queue[id];) {
      memcpy (thread_intents[id], thread_queue_head[id],
	      BYTE_COUNT_A + BYTE_COUNT_O);
      thread_queue_head[id] += BYTE_COUNT_A + BYTE_COUNT_O;
      generate_from_node (thread_intents[id],
			  thread_intents[id] + int_count_a,
			  *(int *) thread_queue_head[id],
			  *(int *) (thread_queue_head[id] + INT_SIZE), id);
      thread_queue_head[id] += INT_SIZE << 1;
    }
  }
  return;
}

void
find_all_intents (void)
{
  int i, queue_size, attrs;
  if (para_level <= 0)
    para_level = 1;
  if (para_level > attributes)
    para_level = attributes;
  threads = cpus;
  if (threads <= 0)
    threads = 1;
  if (verbosity_level >= 3)
    fprintf (stderr, "INFO: running in %i threads\n", threads);
  counts =
    (struct thread_stat *) calloc (threads, sizeof (struct thread_stat));
  thread_id = (thread_id_t *) malloc (sizeof (thread_id_t) * threads);
  memset (thread_id, 0, sizeof (thread_id_t) * threads);
  thread_i = (int *) malloc (sizeof (int) * threads);
  thread_queue =
    (unsigned char **) malloc (sizeof (unsigned char *) * threads);
  thread_queue_head =
    (unsigned char **) malloc (sizeof (unsigned char *) * threads);
  thread_queue_limit =
    (unsigned char **) malloc (sizeof (unsigned char *) * threads);
  thread_intents =
    (unsigned long **) malloc (sizeof (unsigned long *) * threads);
  queue_size = attributes;
  queue_size = queue_size / threads + 1;
  attrs = attributes - para_level + 1;
  for (i = 0; i < threads; i++) {
    thread_i[i] = i;
    thread_queue_head[i] = thread_queue[i] = (unsigned char *)
      malloc ((BYTE_COUNT_A + BYTE_COUNT_O + (INT_SIZE << 1)) * queue_size);
    thread_queue_limit[i] =
      thread_queue_head[i] + (BYTE_COUNT_A + BYTE_COUNT_O +
			      (INT_SIZE << 1)) * queue_size;
    thread_intents[i] =
      (unsigned long *) malloc ((BYTE_COUNT_A + BYTE_COUNT_O) * attrs);
  }
  compute_closure (thread_intents[0], thread_intents[0] + int_count_a,
		   NULL, NULL, NULL);
  counts[0].closures++;
  counts[0].computed++;
  print_attributes (thread_intents[0]);
  if (thread_intents[0][int_count_a - 1] & BIT)
    return;
  parallel_generate_from_node (thread_intents[0],
			       thread_intents[0] + int_count_a, 0,
			       ARCHBIT, 0, 0);
  print_thread_info (1, counts[0]);
  for (i = 1; i < threads; i++) {
    if (thread_id[i]) {
#ifdef WINNT
      WaitForSingleObject (thread_id[i], INFINITE);
      CloseHandle (thread_id[i]);
#else
      pthread_join (thread_id[i], NULL);
#endif
    }
    print_thread_info (i + 1, counts[i]);
    counts[0].computed += counts[i].computed;
    counts[0].closures += counts[i].closures;
  }
  counts[0].computed += initial_thr_stat.computed;
  counts[0].closures += initial_thr_stat.closures;
  if (verbosity_level >= 3)
    fprintf (stderr, "INFO: total %7i closures\n", counts[0].closures);
  if (verbosity_level >= 3)
    fprintf (stderr, "INFO: total %7i concepts\n", counts[0].computed);
}

int
main (argc, argv)
     int argc;
     char **argv;
{
  in_file = stdin;
  out_file = stdout;
  if (argc > 1) {
    int index = 1;
    for (; (index < argc &&
	    argv[index][0] == '-' && argv[index][1] != 0); index++) {
      switch (argv[index][1]) {
      case 'S':
	min_support = atoi (argv[index] + 2);
	break;
      case 'P':
	cpus = atoi (argv[index] + 2);
	break;
      case 'L':
	para_level = atoi (argv[index] + 2) - 1;
	break;
      case 'V':
	verbosity_level = atoi (argv[index] + 2);
	break;
      default:
	attr_offset = atoi (argv[index] + 1);
      }
    }
    if ((argc > index) && (argv[index][0] != '-'))
      in_file = fopen (argv[index], "rb");
    if ((argc > index + 1) && (argv[index + 1][0] != '-'))
      out_file = fopen (argv[index + 1], "wb");
  }
  if (!in_file) {
    fprintf (stderr, "%s: cannot open input data stream\n", argv[0]);
    return 1;
  }
  if (!out_file) {
    fprintf (stderr, "%s: open output data stream\n", argv[0]);
    return 2;
  }
#ifdef WINNT
  output_lock = CreateMutex (NULL, FALSE, NULL);
#endif
  read_context (in_file);
  fclose (in_file);
  print_context_info ();
  initialize_algorithm ();
  find_all_intents ();
  fclose (out_file);
  return 0;
}
