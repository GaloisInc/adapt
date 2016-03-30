from PyPDF2 import PdfFileMerger, PdfFileReader
from StringIO import StringIO


class PdfWriter(object):

    def __init__(self, *dots):
        self.merger = PdfFileMerger()
        for dot in dots:
            self.append(dot)

    def append(self, dot):
        stream = StringIO(dot.pipe(format='pdf'))
        self.merger.append(stream)

    def write(self, filename):
        self.merger.write(filename)
