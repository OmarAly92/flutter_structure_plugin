import 'dart:io';

void main() {
  final dir = Directory('lib/core');
  if (dir.existsSync()) {
    dir.deleteSync(recursive: true);
  }
}
