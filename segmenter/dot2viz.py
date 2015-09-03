import os
import argparse

def processing(dir, output):
    files = [f for f in os.listdir(dir) if os.path.isfile(f) and f.endswith(".dot")]
    for f in files:
        print "Processing " + f
        os.system("dot -T " + output + " -O " + f)

if __name__ == "__main__":
    # Read file
    parser = argparse.ArgumentParser(description='Convert all dot files in a given directory to other format')
    parser.add_argument('--format', '-f', nargs='?', default='pdf', type=str, help='pdf/png/ps')
    parser.add_argument('--dir', '-d', nargs='?', default='.', type=str, help='directory with dot files')

    args = parser.parse_args()
    processing(args.dir, args.format)
