from PyPDF2 import PdfFileMerger, PdfFileReader
from io import BytesIO


class PdfWriter(object):

    def __init__(self, *dots):
        self.merger = PdfFileMerger()
        for dot in dots:
            self.append(dot)

    def append(self, dot):
        stream = BytesIO(dot.pipe(format='pdf'))
        self.merger.append(stream)

    def append_dx(self, graph, dx):
        dot = graph.generate_dot(symptoms=dx.symptoms)
        self.append(dot)

        dot = graph.generate_dot(dxs=dx.reduced_diagnosis(),
                                 symptoms=dx.symptoms,
                                 label='DX Summary')
        self.append(dot)
        for path, match in dx.iterate():
            dot = graph.generate_dot(path=path,
                                     match=match,
                                     symptoms=dx.symptoms,
                                     label='DX Match')
            self.append(dot)

    def write(self, filename):
        self.merger.write(filename)

    def clear(self):
        self.merger.close()
        self.merger = PdfFileMerger()
