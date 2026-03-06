default:
	just --list

package name="pa.zip":
	rm -f "{{name}}"
	zip -r "{{name}}" pa
